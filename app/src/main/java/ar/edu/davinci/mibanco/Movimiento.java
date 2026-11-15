package ar.edu.davinci.mibanco;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de un movimiento de la cuenta.
 * Se lee y escribe en Firestore en usuarios/{uid}/movimientos.
 * Implementa Parcelable para poder viajar dentro de un Bundle hacia DetalleMovimientoActivity.
 */
public class Movimiento implements Parcelable {

    // Nombres de los campos en Firestore
    public static final String CAMPO_TITULO = "titulo";
    public static final String CAMPO_MONTO = "monto";
    public static final String CAMPO_ES_INGRESO = "esIngreso";
    public static final String CAMPO_AVATAR = "avatarUrl";
    public static final String CAMPO_FECHA = "fecha";

    private final String id;
    private final String avatarUrl;
    private final String titulo;
    private final Date fecha;
    private final double monto;
    private final boolean esIngreso;

    public Movimiento(String id, String avatarUrl, String titulo, Date fecha,
                      double monto, boolean esIngreso) {
        this.id = id;
        this.avatarUrl = avatarUrl;
        this.titulo = titulo;
        this.fecha = fecha;
        this.monto = monto;
        this.esIngreso = esIngreso;
    }

    /** Construye un Movimiento a partir de un documento de Firestore. */
    public static Movimiento desdeDocumento(DocumentSnapshot doc) {
        // ESTIMATE: mientras el servidor no confirma la escritura,
        // usa la hora local en lugar de devolver null
        Timestamp ts = doc.getTimestamp(CAMPO_FECHA,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        Double monto = doc.getDouble(CAMPO_MONTO);

        return new Movimiento(
                doc.getId(),
                doc.getString(CAMPO_AVATAR),
                doc.getString(CAMPO_TITULO),
                ts != null ? ts.toDate() : new Date(),
                monto != null ? monto : 0,
                Boolean.TRUE.equals(doc.getBoolean(CAMPO_ES_INGRESO)));
    }

    /** Convierte el movimiento al formato que se guarda en Firestore. */
    public Map<String, Object> aMapa() {
        Map<String, Object> datos = new HashMap<>();
        datos.put(CAMPO_TITULO, titulo);
        datos.put(CAMPO_MONTO, monto);
        datos.put(CAMPO_ES_INGRESO, esIngreso);
        datos.put(CAMPO_AVATAR, avatarUrl);
        datos.put(CAMPO_FECHA, new Timestamp(fecha));
        return datos;
    }

    // ---------- Parcelable ----------

    /** Reconstruye el objeto leyendo los campos en el MISMO orden en que se escribieron. */
    protected Movimiento(Parcel in) {
        id = in.readString();
        avatarUrl = in.readString();
        titulo = in.readString();
        fecha = new Date(in.readLong());
        monto = in.readDouble();
        esIngreso = in.readByte() != 0;
    }

    /** Aplana el objeto en el Parcel, campo por campo. */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(avatarUrl);
        dest.writeString(titulo);
        dest.writeLong(fecha.getTime());
        dest.writeDouble(monto);
        dest.writeByte((byte) (esIngreso ? 1 : 0)); // writeBoolean requiere API 29
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Android usa CREATOR para crear instancias al desempaquetar el Bundle. */
    public static final Creator<Movimiento> CREATOR = new Creator<Movimiento>() {
        @Override
        public Movimiento createFromParcel(Parcel in) {
            return new Movimiento(in);
        }

        @Override
        public Movimiento[] newArray(int size) {
            return new Movimiento[size];
        }
    };

    // ---------- Getters ----------

    public String getId() { return id; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getTitulo() { return titulo; }
    public Date getFecha() { return fecha; }
    public double getMonto() { return monto; }
    public boolean esIngreso() { return esIngreso; }
}