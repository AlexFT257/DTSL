import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.dtls.R

class Settings : Fragment(), FontSizeChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el diseño de fragment_settings.xml
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa las preferencias compartidas
        sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Encuentra vistas en el diseño
        val btnOpenFontSettings = view.findViewById<Button>(R.id.btnOpenFontSettings)
        val textViewExample = view.findViewById<TextView>(R.id.textViewExample)
        val btnChangeTheme = view.findViewById<Button>(R.id.btnChangeTheme)

        // Manejar el clic en el botón "Aplicar"
        btnOpenFontSettings.setOnClickListener {
            // Abre el diálogo de configuración de fuente
            val fontSettingsDialog = FontSettingsDialog(sharedPreferences, this)
            fontSettingsDialog.show(parentFragmentManager, "FontSettingsDialog")
        }

        // Manejar el clic en el botón "Cambiar Tema"
        btnChangeTheme.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Cambiar al tema opuesto al modo actual
            val newNightMode = when (currentNightMode) {
                Configuration.UI_MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
                Configuration.UI_MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Opcional, seguir el modo del sistema
            }

            // Establecer el nuevo modo de noche
            AppCompatDelegate.setDefaultNightMode(newNightMode)

            // Reiniciar la actividad o fragmento actual para aplicar el cambio de tema
            requireActivity().recreate()
        }

        // Aplicar el tamaño de fuente predeterminado al cargar el fragmento
        applyFontSize(getFontSizeFromPreferences(), textViewExample)
    }

    // Función para aplicar el tamaño de fuente en tus elementos de vista
    private fun applyFontSize(fontSize: Int, textViewExample: TextView) {
        // Ajusta el tamaño de fuente en función de `fontSize`
        when (fontSize) {
            FontSettingsDialog.SMALL_FONT_SIZE -> {
                textViewExample.textSize = 14f // Tamaño pequeño
                textViewExample.text = "Texto de ejemplo (Pequeño)"
            }
            FontSettingsDialog.MEDIUM_FONT_SIZE -> {
                textViewExample.textSize = 18f // Tamaño mediano
                textViewExample.text = "Texto de ejemplo (Mediano)"
            }
            FontSettingsDialog.LARGE_FONT_SIZE -> {
                textViewExample.textSize = 24f // Tamaño grande
                textViewExample.text = "Texto de ejemplo (Grande)"
            }
            else -> {
                // Tamaño de fuente predeterminado si no se encuentra el valor en las preferencias
                textViewExample.textSize = 18f
                textViewExample.text = "Texto de ejemplo (Mediano)"
            }
        }
    }

    // Función para obtener el tamaño de fuente de las preferencias compartidas
    private fun getFontSizeFromPreferences(): Int {
        // Obtén el tamaño de fuente de las preferencias compartidas (deberías implementar esto)
        // Por ejemplo, si almacenaste el tamaño de fuente en las preferencias como "fontSize":
        return sharedPreferences.getInt(FontSettingsDialog.KEY_FONT_SIZE, FontSettingsDialog.MEDIUM_FONT_SIZE)
    }

    // Implementar la interfaz FontSizeChangeListener
    override fun onFontSizeChanged(fontSize: Int) {
        // Aquí puedes reaccionar a cambios en el tamaño de fuente si es necesario
        // Por ejemplo, podrías volver a cargar la vista con el nuevo tamaño de fuente
        applyFontSize(fontSize, requireView().findViewById(R.id.textViewExample))
    }
}
