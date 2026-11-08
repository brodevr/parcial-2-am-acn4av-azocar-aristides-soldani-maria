package ar.edu.davinci.mibanco;

import android.content.Intent;
import android.os.Bundle;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvMovimientos;
    private MovimientoAdapter adapter;
    private TextView tvSaldo;
    private ImageView btnOjo;
    private Button btnTransferir;
    private Button btnAlias;
    private TextView tvVerTodos;
    private List<Movimiento> movimientos;

    private double saldo = 245680.50;
    private boolean saldoVisible = true;

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

                        // Elemento creado dinamicamente desde Java
                        Movimiento nuevo = new Movimiento(
                                R.drawable.avatar_default,
                                getString(R.string.mov_transferencia_enviada) + " a " + destino,
                                getString(R.string.ahora),
                                monto,
                                false);

                        adapter.agregarMovimiento(nuevo);
                        rvMovimientos.smoothScrollToPosition(0);

                        saldo -= monto;
                        actualizarSaldo();

                        Toast.makeText(this, R.string.msg_transferencia_ok,
                                Toast.LENGTH_SHORT).show();
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

        rvMovimientos = findViewById(R.id.rvMovimientos);
        tvSaldo = findViewById(R.id.tvSaldo);
        btnOjo = findViewById(R.id.btnOjo);
        btnTransferir = findViewById(R.id.btnTransferir);
        btnAlias = findViewById(R.id.btnAlias);
        tvVerTodos = findViewById(R.id.tvVerTodos);
        TextView tvSaludo = findViewById(R.id.tvSaludo);
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        ImageView btnLogout = findViewById(R.id.btnLogout);

        String nombre = usuario.getDisplayName();
        if (nombre == null || nombre.isEmpty()) {
            nombre = getString(R.string.saludo_generico);
        }
        tvSaludo.setText(getString(R.string.saludo_nombre, primerNombre(nombre)));
        tvAvatar.setText(iniciales(nombre));

        btnLogout.setOnClickListener(v -> confirmarLogout());


        movimientos = crearMovimientos();
        adapter = new MovimientoAdapter(movimientos);
        rvMovimientos.setLayoutManager(new LinearLayoutManager(this));
        rvMovimientos.setAdapter(adapter);

        actualizarSaldo();

        tvVerTodos.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovimientosActivity.class);
            intent.putExtra(MovimientosActivity.EXTRA_MOVIMIENTOS,
                    new ArrayList<>(movimientos));
            startActivity(intent);
        });

        // Mostrar / ocultar el saldo
        btnOjo.setOnClickListener(v -> {
            saldoVisible = !saldoVisible;
            actualizarSaldo();
        });

        btnTransferir.setOnClickListener(v -> {
            Intent i = new Intent(this, TransferirActivity.class);
            i.putExtra(TransferirActivity.EXTRA_SALDO, saldo);
            transferirLauncher.launch(i);
        });

        btnAlias.setOnClickListener(v ->
                startActivity(new Intent(this, AliasActivity.class)));
    }

    private void actualizarSaldo() {
        if (saldoVisible) {
            tvSaldo.setText(formatearPesos(saldo));
        } else {
            tvSaldo.setText(R.string.saldo_oculto);
        }
    }

    static String formatearPesos(double monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "$ " + formato.format(monto);
    }

    private List<Movimiento> crearMovimientos() {
        List<Movimiento> lista = new ArrayList<>();
        lista.add(new Movimiento(R.drawable.avatar_martina,
                getString(R.string.mov_recibida), "Hoy, 10:32", 25000, true));
        lista.add(new Movimiento(R.drawable.avatar_comercio,
                getString(R.string.mov_farmacia), "Hoy, 09:15", 4320, false));
        lista.add(new Movimiento(R.drawable.avatar_comercio,
                getString(R.string.mov_supermercado), "Ayer, 18:45", 18450, false));
        lista.add(new Movimiento(R.drawable.avatar_sofia,
                getString(R.string.mov_reembolso), "Ayer, 12:10", 2100, true));
        lista.add(new Movimiento(R.drawable.avatar_comercio,
                getString(R.string.mov_streaming), "11 sep., 08:00", 3990, false));
        lista.add(new Movimiento(R.drawable.avatar_empresa,
                getString(R.string.mov_sueldo), "9 sep., 07:30", 180000, true));
        lista.add(new Movimiento(R.drawable.avatar_lucas,
                "Transferencia a Lucas", "8 sep., 16:20", 12000, false));
        lista.add(new Movimiento(R.drawable.avatar_comercio,
                getString(R.string.mov_supermercado), "6 sep., 11:05", 9870, false));
        return lista;
    }

        private void confirmarLogout () {
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

        private void irALogin () {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        private static String primerNombre (String nombre){
            return nombre.trim().split("\\s+")[0];
        }

        private static String iniciales (String nombre){
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
