package ar.edu.davinci.mibanco;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private BancoRepositorio repo;
    private final List<Movimiento> movimientos = new ArrayList<>();
    private MovimientoAdapter adapter;

    private RecyclerView rvMovimientos;
    private TextView tvSaldo;
    private TextView tvSinMovimientos;

    private String nombre;
    private String email;

    private double saldo;
    private boolean saldoCargado = false;
    private boolean saldoVisible = true;
    private boolean inicializando = false;

    // Recibe el resultado de TransferirActivity
    private final ActivityResultLauncher<Intent> transferirLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() != RESULT_OK || resultado.getData() == null) {
                            return;
                        }
                        String destino = resultado.getData()
                                .getStringExtra(TransferirActivity.EXTRA_DESTINO);
                        double monto = resultado.getData()
                                .getDoubleExtra(TransferirActivity.EXTRA_MONTO, 0);
                        transferir(destino, monto);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            irALogin();
            return;
        }

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repo = new BancoRepositorio(usuario.getUid());
        email = usuario.getEmail();
        nombre = usuario.getDisplayName();
        if (nombre == null || nombre.isEmpty()) {
            nombre = getString(R.string.saludo_generico);
        }

        rvMovimientos = findViewById(R.id.rvMovimientos);
        tvSaldo = findViewById(R.id.tvSaldo);
        tvSinMovimientos = findViewById(R.id.tvSinMovimientos);
        ImageView btnOjo = findViewById(R.id.btnOjo);
        Button btnTransferir = findViewById(R.id.btnTransferir);
        Button btnAlias = findViewById(R.id.btnAlias);
        TextView tvVerTodos = findViewById(R.id.tvVerTodos);
        TextView tvSaludo = findViewById(R.id.tvSaludo);
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        ImageView btnLogout = findViewById(R.id.btnLogout);

        tvSaludo.setText(getString(R.string.saludo_nombre, primerNombre(nombre)));
        tvAvatar.setText(iniciales(nombre));

        adapter = new MovimientoAdapter(movimientos);
        rvMovimientos.setLayoutManager(new LinearLayoutManager(this));
        rvMovimientos.setAdapter(adapter);

        actualizarSaldo();
        escucharSaldo();
        escucharMovimientos();

        btnOjo.setOnClickListener(v -> {
            saldoVisible = !saldoVisible;
            actualizarSaldo();
        });

        btnTransferir.setOnClickListener(v -> {
            if (!saldoCargado) {
                return; // todavía no llegó el saldo de Firestore
            }
            Intent i = new Intent(this, TransferirActivity.class);
            i.putExtra(TransferirActivity.EXTRA_SALDO, saldo);
            transferirLauncher.launch(i);
        });

        btnAlias.setOnClickListener(v ->
                startActivity(new Intent(this, AliasActivity.class)));

        tvVerTodos.setOnClickListener(v ->
                startActivity(new Intent(this, MovimientosActivity.class)));

        btnLogout.setOnClickListener(v -> confirmarLogout());
    }

    /**
     * Escucha en tiempo real el documento del usuario.
     * Pasar "this" hace que el listener se desconecte solo cuando la Activity se detiene.
     */
    private void escucharSaldo() {
        repo.usuario().addSnapshotListener(this, (doc, error) -> {
            if (error != null) {
                Toast.makeText(this, R.string.msg_error_carga, Toast.LENGTH_SHORT).show();
                return;
            }
            if (doc == null) {
                return;
            }
            inicializarSiHaceFalta(doc);

            Double valor = doc.getDouble(BancoRepositorio.CAMPO_SALDO);
            if (valor != null) {
                saldo = valor;
                saldoCargado = true;
                actualizarSaldo();
            }
        });
    }

    /** Primera entrada del usuario: crea los movimientos de demostración. */
    private void inicializarSiHaceFalta(DocumentSnapshot doc) {
        boolean listo = doc.exists() && Boolean.TRUE.equals(doc.getBoolean(BancoRepositorio.CAMPO_DEMO));
        // Un "no existe" leído del caché local puede ser falso: esperamos al servidor
        boolean dudoso = !doc.exists() && doc.getMetadata().isFromCache();
        if (listo || dudoso || inicializando) {
            return;
        }
        inicializando = true;
        repo.inicializarCuenta(this, doc.exists(), nombre, email)
                .addOnCompleteListener(this, t -> inicializando = false);
    }

    /**
     * Escucha los últimos movimientos. En lugar de recargar toda la lista,
     * aplica solo los cambios que informa Firestore, con su animación.
     */
    private void escucharMovimientos() {
        repo.ultimosMovimientos().addSnapshotListener(this, (snap, error) -> {
            if (error != null || snap == null) {
                return;
            }
            aplicarCambios(snap);
            tvSinMovimientos.setVisibility(movimientos.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void aplicarCambios(QuerySnapshot snap) {
        for (DocumentChange cambio : snap.getDocumentChanges()) {
            Movimiento m = Movimiento.desdeDocumento(cambio.getDocument());
            int anterior = cambio.getOldIndex();
            int nuevo = cambio.getNewIndex();

            switch (cambio.getType()) {
                case ADDED:
                    movimientos.add(nuevo, m);
                    adapter.notifyItemInserted(nuevo);
                    break;
                case MODIFIED:
                    // Pasa, por ejemplo, cuando el servidor confirma la fecha real
                    movimientos.remove(anterior);
                    movimientos.add(nuevo, m);
                    if (anterior != nuevo) {
                        adapter.notifyItemMoved(anterior, nuevo);
                    }
                    adapter.notifyItemChanged(nuevo);
                    break;
                case REMOVED:
                    // El 6.º movimiento sale del límite de 5 cuando entra uno nuevo
                    movimientos.remove(anterior);
                    adapter.notifyItemRemoved(anterior);
                    break;
            }
        }
    }

    private void transferir(String destino, double monto) {
        String titulo = getString(R.string.mov_transferencia_a, destino);
        // La UI se actualiza sola por los listeners; acá solo informamos el resultado
        repo.transferir(titulo, destino, monto)
                .addOnSuccessListener(this, v -> {
                    rvMovimientos.smoothScrollToPosition(0);
                    Toast.makeText(this, R.string.msg_transferencia_ok, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this, R.string.msg_transferencia_error, Toast.LENGTH_LONG).show());
    }

    private void actualizarSaldo() {
        if (!saldoCargado) {
            tvSaldo.setText(R.string.saldo_cargando);
        } else if (saldoVisible) {
            tvSaldo.setText(formatearPesos(saldo));
        } else {
            tvSaldo.setText(R.string.saldo_oculto);
        }
    }

    static String formatearPesos(double monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "$ " + formato.format(monto);
    }

    private void confirmarLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_logout_titulo)
                .setMessage(R.string.dialog_logout_mensaje)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .setPositiveButton(R.string.btn_cerrar_sesion, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, R.string.msg_sesion_cerrada, Toast.LENGTH_SHORT).show();
                    irALogin();
                })
                .show();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String primerNombre(String nombre) {
        return nombre.trim().split("\\s+")[0];
    }

    private static String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) {
                resultado.append(Character.toUpperCase(partes[i].charAt(0)));
            }
        }
        return resultado.toString();
    }
}