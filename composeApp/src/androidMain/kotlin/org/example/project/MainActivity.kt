package org.example.project

import App
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import org.example.project.Datos.GestorAuth

class MainActivity : ComponentActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    // El "escuchador" que espera a que el usuario elija su cuenta de Google
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Recuperamos la cuenta de Google seleccionada
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    // Iniciamos sesión en Firebase de forma invisible
                    autenticarEnFirebase(idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Pasamos nuestra función nativa hacia el mundo multiplataforma
            App(
                onLoginGoogle = { lanzarVentanaGoogle() }
            )
        }
    }

    private fun lanzarVentanaGoogle() {
        // Obtenemos el ID de cliente de Google del archivo google-services.json automáticamente
        val webClientId = getString(resources.getIdentifier("default_web_client_id", "string", packageName))

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut() // Desvinculamos sesiones atascadas por si acaso

        // ¡Abrimos la ventana nativa de elegir cuenta!
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun autenticarEnFirebase(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ¡Avisamos a nuestro KMP que el usuario ha cambiado!
                    lifecycleScope.launch {
                        GestorAuth.actualizarUsuarioActual()
                    }
                    Toast.makeText(this, "\uD83D\uDC4D", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error witch data base", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}