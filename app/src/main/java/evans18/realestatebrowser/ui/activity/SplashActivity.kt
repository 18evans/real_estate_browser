package evans18.realestatebrowser.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MILLIS = 800L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }


}