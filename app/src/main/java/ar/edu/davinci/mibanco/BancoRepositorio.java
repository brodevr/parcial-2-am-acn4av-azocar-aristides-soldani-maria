package ar.edu.davinci.mibanco;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Acceso a los datos de un usuario en Firestore.
 */
public class BancoRepositorio {

    public static final String CAMPO_SALDO = "saldo";
    public static final String CAMPO_DEMO = "demoCargado";
    public static final double SALDO_INICIAL = 250000.0;
    public static final int LIMITE_HOME = 5;

    private static final String USUARIOS = "usuarios";
    private static final String MOVIMIENTOS = "movimientos";

    private static final long HORA = 60L * 60 * 1000;
    private static final long DIA = 24 * HORA;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public BancoRepositorio(String uid) {
        this.uid = uid;
    }

    public DocumentReference usuario() {
        return db.collection(USUARIOS).document(uid);
    }

    private CollectionReference movimientos() {
        return usuario().collection(MOVIMIENTOS);
    }

    public Query ultimosMovimientos() {
        return movimientos()
                .orderBy(Movimiento.CAMPO_FECHA, Query.Direction.DESCENDING)
                .limit(LIMITE_HOME);
    }

    public Query todosLosMovimientos() {
        return movimientos().orderBy(Movimiento.CAMPO_FECHA, Query.Direction.DESCENDING);
    }

    /**
     * Descuenta el saldo y registra el movimiento en una sola operación atómica:
     * si una de las dos escrituras falla, no se aplica ninguna.
     */
    public Task<Void> transferir(String titulo, String destino, double monto) {
        Map<String, Object> mov = new HashMap<>();
        mov.put(Movimiento.CAMPO_TITULO, titulo);
        mov.put(Movimiento.CAMPO_MONTO, monto);
        mov.put(Movimiento.CAMPO_ES_INGRESO, false);
        mov.put(Movimiento.CAMPO_AVATAR, avatarIniciales(destino, "0A3D62"));
        mov.put(Movimiento.CAMPO_FECHA, FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.update(usuario(), CAMPO_SALDO, FieldValue.increment(-monto));
        batch.set(movimientos().document(), mov);
        return batch.commit();
    }

    /**
     * Carga datos de demostración la primera vez que el usuario entra.
     * También repara cuentas cuyo documento no llegó a crearse en el registro.
     */
    public Task<Void> inicializarCuenta(Context ctx, boolean existe,
                                        String nombre, String email) {
        WriteBatch batch = db.batch();

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put(CAMPO_DEMO, true);
        if (!existe) {
            datosUsuario.put("nombre", nombre);
            datosUsuario.put("email", email);
            datosUsuario.put(CAMPO_SALDO, SALDO_INICIAL);
            datosUsuario.put("creado", FieldValue.serverTimestamp());
        }
        batch.set(usuario(), datosUsuario, SetOptions.merge());

        long ahora = System.currentTimeMillis();
        Movimiento[] demo = {
                demo(foto(47), ctx.getString(R.string.mov_recibida), ahora - HORA, 25000, true),
                demo(avatarIniciales(ctx.getString(R.string.mov_farmacia), "1ABC9C"),
                        ctx.getString(R.string.mov_farmacia), ahora - 3 * HORA, 4320, false),
                demo(avatarIniciales(ctx.getString(R.string.mov_supermercado), "1ABC9C"),
                        ctx.getString(R.string.mov_supermercado), ahora - DIA, 18450, false),
                demo(foto(32), ctx.getString(R.string.mov_reembolso), ahora - DIA - 5 * HORA, 2100, true),
                demo(avatarIniciales(ctx.getString(R.string.mov_streaming), "1ABC9C"),
                        ctx.getString(R.string.mov_streaming), ahora - 2 * DIA, 3990, false),
                demo(avatarIniciales("Da Vinci", "F5C469"),
                        ctx.getString(R.string.mov_sueldo), ahora - 4 * DIA, 180000, true),
                demo(foto(12), ctx.getString(R.string.mov_transferencia_a, "Lucas"),
                        ahora - 5 * DIA, 12000, false),
                demo(avatarIniciales(ctx.getString(R.string.mov_supermercado), "1ABC9C"),
                        ctx.getString(R.string.mov_supermercado), ahora - 7 * DIA, 9870, false),
        };
        for (Movimiento m : demo) {
            batch.set(movimientos().document(), m.aMapa());
        }

        return batch.commit();
    }

    private static Movimiento demo(String avatar, String titulo, long fecha,
                                   double monto, boolean ingreso) {
        return new Movimiento(null, avatar, titulo, new Date(fecha), monto, ingreso);
    }

    /** Foto de persona desde pravatar.cc (imágenes libres). */
    private static String foto(int numero) {
        return "https://i.pravatar.cc/150?img=" + numero;
    }

    /** Imagen generada con las iniciales del nombre, desde ui-avatars.com. */
    static String avatarIniciales(String nombre, String colorFondo) {
        return "https://ui-avatars.com/api/?size=128&bold=true&color=ffffff"
                + "&background=" + colorFondo
                + "&name=" + Uri.encode(nombre);
    }
}