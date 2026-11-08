package ar.edu.davinci.mibanco;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de un movimiento de la cuenta.
 * Se lee y escribe en Firestore en usuarios/{uid}/movimientos.
 */
public class Movimiento implements Serializable {

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

    public String getId() { return id; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getTitulo() { return titulo; }
    public Date getFecha() { return fecha; }
    public double getMonto() { return monto; }
    public boolean esIngreso() { return esIngreso; }
}