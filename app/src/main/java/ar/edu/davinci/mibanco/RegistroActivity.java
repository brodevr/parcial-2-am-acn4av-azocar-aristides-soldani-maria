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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Alta de cuenta: crea el usuario en Firebase Auth y su documento en Firestore.
 */
public class RegistroActivity extends AppCompatActivity {

    private static final double SALDO_INICIAL = 250000.0;
    private static final int PASSWORD_MINIMA = 6; // mínimo que exige Firebase Auth

    private FirebaseAuth auth;

    private TextInputLayout tilNombre;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmar;
    private TextInputEditText etNombre;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmar;
    private Button btnRegistrar;
    private ProgressBar progressRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRegistro), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        tilNombre = findViewById(R.id.tilNombre);
        tilEmail = findViewById(R.id.tilEmailRegistro);
        tilPassword = findViewById(R.id.tilPasswordRegistro);
        tilConfirmar = findViewById(R.id.tilPasswordConfirmar);
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmailRegistro);
        etPassword = findViewById(R.id.etPasswordRegistro);
        etConfirmar = findViewById(R.id.etPasswordConfirmar);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressRegistro = findViewById(R.id.progressRegistro);
        TextView tvIrLogin = findViewById(R.id.tvIrLogin);

        btnRegistrar.setOnClickListener(v -> registrar());
        tvIrLogin.setOnClickListener(v -> finish());
    }

    private void registrar() {
        tilNombre.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmar.setError(null);

        String nombre = LoginActivity.texto(etNombre);
        String email = LoginActivity.texto(etEmail);
        String password = LoginActivity.texto(etPassword);
        String confirmar = LoginActivity.texto(etConfirmar);

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.msg_nombre_vacio));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.msg_email_invalido));
            return;
        }
        if (password.length() < PASSWORD_MINIMA) {
            tilPassword.setError(getString(R.string.msg_password_corta));
            return;
        }
        if (!password.equals(confirmar)) {
            tilConfirmar.setError(getString(R.string.msg_passwords_distintas));
            return;
        }

        setCargando(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, resultado -> {
                    FirebaseUser usuario = resultado.getUser();
                    if (usuario == null) {
                        mostrarError(R.string.msg_registro_error);
                        return;
                    }

                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();

                    // Esperamos a que se guarde el nombre antes de seguir,
                    // así la Home ya lo encuentra al abrir
                    usuario.updateProfile(perfil).addOnCompleteListener(this, t ->
                            crearDocumentoUsuario(usuario.getUid(), nombre, email));
                })
                .addOnFailureListener(this, e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        mostrarError(R.string.msg_email_en_uso);
                    } else {
                        mostrarError(R.string.msg_registro_error);
                    }
                });
    }

    private void crearDocumentoUsuario(String uid, String nombre, String email) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("email", email);
        datos.put("saldo", SALDO_INICIAL);
        datos.put("creado", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .set(datos)
                .addOnSuccessListener(this, v -> irAHome())
                .addOnFailureListener(this, e -> mostrarError(R.string.msg_registro_error));
    }

    private void mostrarError(int mensaje) {
        setCargando(false);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void setCargando(boolean cargando) {
        progressRegistro.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnRegistrar.setEnabled(!cargando);
    }

    private void irAHome() {
        Intent intent = new Intent(this, MainActivity.class);
        // Borra Login y Registro del historial: "atrás" desde la Home cierra la app
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}