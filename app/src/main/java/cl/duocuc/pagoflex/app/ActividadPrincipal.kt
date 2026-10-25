package cl.duocuc.pagoflex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duocuc.pagoflex.app.navigation.NavegacionEntrePantallas
import cl.duocuc.pagoflex.app.ui.theme.TemaPagoFlex

class ActividadPrincipal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaPagoFlex {
                NavegacionEntrePantallas()
            }
        }
    }
}
