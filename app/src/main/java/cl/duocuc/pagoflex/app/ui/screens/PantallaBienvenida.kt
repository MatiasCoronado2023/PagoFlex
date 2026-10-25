package cl.duocuc.pagoflex.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.ui.theme.Primario

@Composable
fun PantallaBienvenida() {
    Box(Modifier.fillMaxSize().background(Primario), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = stringResource(R.string.cd_logo), modifier = Modifier.size(96.dp))
            Text("PagoFlex", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("Revisa y paga tus cuentas", color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
        Text(
            "Proyecto académico · DSY1105", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)
        )
    }
}
