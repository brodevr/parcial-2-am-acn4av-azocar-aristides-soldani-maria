package ar.edu.davinci.mibanco;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Comprobante de un movimiento. Recibe el Movimiento completo empaquetado en un Bundle.
 */
public class DetalleMovimientoActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIMIENTO = "extra_movimiento";

    private static final int LARGO_OPERACION = 10;

    /**
     * Arma el paquete (Bundle) con el movimiento y abre la pantalla.
     * Lo usan tanto la Home como el historial completo.
     */
    public static void abrir(Context context, Movimiento movimiento) {
        Bundle paquete = new Bundle();
        paquete.putParcelable(EXTRA_MOVIMIENTO, movimiento);

        Intent intent = new Intent(context, DetalleMovimientoActivity.class);
        intent.putExtras(paquete);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_movimiento);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainDetalle), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Desempaquetamos el Bundle. BundleCompat evita el getParcelable() deprecado en API 33
        Bundle paquete = getIntent().getExtras();
        Movimiento movimiento = paquete == null ? null
                : BundleCompat.getParcelable(paquete, EXTRA_MOVIMIENTO, Movimiento.class);
        if (movimiento == null) {
            finish();
            return;
        }

        mostrar(movimiento);

        ImageView btnVolver = findViewById(R.id.btnVolverDetalle);
        btnVolver.setOnClickListener(v -> finish());

        Button btnCompartir = findViewById(R.id.btnCompartir);
        btnCompartir.setOnClickListener(v -> compartir(movimiento));
    }

    private void mostrar(Movimiento m) {
        ImageView ivAvatar = findViewById(R.id.ivAvatarDetalle);
        TextView tvTitulo = findViewById(R.id.tvTituloDetalle);
        TextView tvMonto = findViewById(R.id.tvMontoDetalle);
        TextView tvTipo = findViewById(R.id.tvTipoDetalle);
        TextView tvFecha = findViewById(R.id.tvFechaDetalle);
        TextView tvOperacion = findViewById(R.id.tvOperacionDetalle);
        TextView tvCuenta = findViewById(R.id.tvCuentaDetalle);

        Glide.with(this)
                .load(m.getAvatarUrl())
                .placeholder(R.drawable.avatar_default)
                .error(R.drawable.avatar_default)
                .centerCrop()
                .into(ivAvatar);

        tvTitulo.setText(m.getTitulo());
        tvMonto.setText(MovimientoAdapter.formatearMonto(m));
        tvMonto.setTextColor(ContextCompat.getColor(this,
                m.esIngreso() ? R.color.teal : R.color.navy));
        tvTipo.setText(m.esIngreso() ? R.string.detalle_ingreso : R.string.detalle_egreso);
        tvFecha.setText(fechaCompleta(m));
        tvOperacion.setText(numeroOperacion(m));
        tvCuenta.setText(R.string.cuenta_principal);
    }

    /** Comparte el comprobante como texto con la app que elija el usuario (Intent implícito). */
    private void compartir(Movimiento m) {
        String texto = getString(R.string.compartir_texto,
                m.getTitulo(),
                MovimientoAdapter.formatearMonto(m),
                fechaCompleta(m),
                numeroOperacion(m));

        Intent enviar = new Intent(Intent.ACTION_SEND);
        enviar.setType("text/plain");
        enviar.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(enviar, getString(R.string.compartir_titulo)));
    }

    private static String fechaCompleta(Movimiento m) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"))
                .format(m.getFecha());
    }

    /** Usamos el ID del documento de Firestore como número de operación. */
    private static String numeroOperacion(Movimiento m) {
        String id = m.getId();
        if (id == null || id.isEmpty()) {
            return "—";
        }
        return id.substring(0, Math.min(LARGO_OPERACION, id.length())).toUpperCase(Locale.ROOT);
    }
}
