package ar.edu.davinci.mibanco;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Historial completo de movimientos, consultado directamente a Firestore.
 */
public class MovimientosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movimientos);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            finish();
            return;
        }

        List<Movimiento> movimientos = new ArrayList<>();
        MovimientoAdapter adapter = new MovimientoAdapter(movimientos);

        RecyclerView rv = findViewById(R.id.rvTodosMovimientos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        ProgressBar progress = findViewById(R.id.progressMovimientos);
        TextView tvVacio = findViewById(R.id.tvSinMovimientosTodos);
        ImageView btnVolver = findViewById(R.id.btnVolverMovimientos);
        btnVolver.setOnClickListener(v -> finish());

        new BancoRepositorio(usuario.getUid())
                .todosLosMovimientos()
                .get()
                .addOnSuccessListener(this, snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        movimientos.add(Movimiento.desdeDocumento(doc));
                    }
                    adapter.notifyItemRangeInserted(0, movimientos.size());
                    progress.setVisibility(View.GONE);
                    tvVacio.setVisibility(movimientos.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(this, e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.msg_error_carga, Toast.LENGTH_SHORT).show();
                });
    }
}