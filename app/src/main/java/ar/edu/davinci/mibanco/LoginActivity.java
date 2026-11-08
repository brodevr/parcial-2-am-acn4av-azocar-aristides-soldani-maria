package ar.edu.davinci.mibanco;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Pantalla de inicio de sesión. Es la launcher de la app:
 * si ya hay una sesión activa, salta directo a la Home.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnIngresar;
    private ProgressBar progressLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLogin), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnIngresar = findViewById(R.id.btnIngresar);
        progressLogin = findViewById(R.id.progressLogin);
        TextView tvIrRegistro = findViewById(R.id.tvIrRegistro);

        btnIngresar.setOnClickListener(v -> ingresar());
        tvIrRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Sesión persistida: Firebase recuerda al usuario entre aperturas de la app
        if (auth.getCurrentUser() != null) {
            irAHome();
        }
    }

    private void ingresar() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = texto(etEmail);
        String password = texto(etPassword);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.msg_email_invalido));
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.msg_password_vacia));
            return;
        }

        setCargando(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setCargando(false);
                    if (task.isSuccessful()) {
                        irAHome();
                    } else {
                        Toast.makeText(this, R.string.msg_login_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setCargando(boolean cargando) {
        progressLogin.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!cargando);
    }

    private void irAHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // para que "atrás" desde la Home no vuelva al login
    }

    static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}