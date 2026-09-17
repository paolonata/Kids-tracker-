package com.kidstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidstracker.ui.AppKidsTracker
import com.kidstracker.ui.KidsViewModel
import com.kidstracker.ui.tema.KidsTema

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: KidsViewModel = viewModel(factory = KidsViewModel.Factory)
            KidsTema(vm.tema) {
                AppKidsTracker(vm)
            }
        }
    }
}
