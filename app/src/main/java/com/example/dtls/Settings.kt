
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.dtls.GestureDetection
import com.example.dtls.GestureDetectionOnline
import com.example.dtls.R
import com.google.android.material.slider.Slider

class Settings : Fragment(), FontSizeChangeListener {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gestureDetectionOnline: GestureDetectionOnline
    private lateinit var gestureDetection: GestureDetection

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
        val textViewTitulo = view.findViewById<TextView>(R.id.ConfigTitle)
        val textViewUmbral = view.findViewById<TextView>(R.id.textView5)
        val textViewIntruccion = view.findViewById<TextView>(R.id.textView6)
        val textViewUso = view.findViewById<TextView>(R.id.textView8)
        val btnChangeTheme = view.findViewById<Button>(R.id.btnChangeTheme)
        val creditText = view.findViewById<TextView>(R.id.textCredits)
        var thresholdSlider = view.findViewById<Slider>(R.id.thresholdSider)
        var textViewInfo = view.findViewById<TextView>(R.id.textView7)

        // para que se pueda acceder al github al enlace
        creditText.movementMethod = LinkMovementMethod.getInstance()

        // buscando las instancias para modificarlas
        gestureDetection = GestureDetection.getInstance(requireContext())
        gestureDetectionOnline = GestureDetectionOnline.getInstance(requireContext())

        // definir OnChange method para el slider
        thresholdSlider.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            gestureDetection.threshold = value/100
            gestureDetectionOnline.threshold = value/100
            Log.println(
                Log.DEBUG,
                "Slider",
                "online: ${gestureDetectionOnline.threshold}, local: ${gestureDetection.threshold}"
            )
        })

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
        applyFontSize(getFontSizeFromPreferences(), textViewExample, textViewTitulo, textViewUmbral, creditText, textViewInfo, textViewIntruccion, textViewUso)
    }

    // Función para aplicar el tamaño de fuente en tus elementos de vista
    private fun applyFontSize(fontSize: Int, textViewExample: TextView, textViewTitulo: TextView, textViewUmbral: TextView, creditText: TextView, textViewInfo: TextView,
                              textViewIntruccion: TextView, textViewUso: TextView) {
        // Ajusta el tamaño de fuente en función de `fontSize`
        when (fontSize) {
            FontSettingsDialog.SMALL_FONT_SIZE -> {
                textViewExample.textSize = 14f // Tamaño pequeño
                textViewTitulo.textSize = 14f
                textViewUmbral.textSize = 14f
                creditText.textSize = 11f
                textViewInfo.textSize = 11f
                textViewIntruccion.textSize = 14f
                textViewUso.textSize = 11f
                textViewExample.text = "Texto de ejemplo (Pequeño)"
            }
            FontSettingsDialog.MEDIUM_FONT_SIZE -> {
                textViewExample.textSize = 18f // Tamaño mediano
                textViewTitulo.textSize = 20f
                textViewUmbral.textSize = 20f
                creditText.textSize = 12f
                textViewInfo.textSize = 12f
                textViewIntruccion.textSize = 20f
                textViewUso.textSize = 12f
                textViewExample.text = "Texto de ejemplo (Mediano)"
            }
            FontSettingsDialog.LARGE_FONT_SIZE -> {
                textViewExample.textSize = 24f // Tamaño grande
                textViewTitulo.textSize = 26f
                textViewUmbral.textSize = 26f
                creditText.textSize = 13f
                textViewInfo.textSize = 13f
                textViewIntruccion.textSize = 26f
                textViewUso.textSize = 13f
                textViewExample.text = "Texto de ejemplo (Grande)"
            }
            else -> {
                // Tamaño de fuente predeterminado si no se encuentra el valor en las preferencias
                textViewExample.textSize = 18f
                textViewTitulo.textSize = 20f
                textViewUmbral.textSize = 20f
                creditText.textSize = 12f
                textViewInfo.textSize = 12f
                textViewIntruccion.textSize = 20f
                textViewUso.textSize = 12f
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
        applyFontSize(fontSize, requireView().findViewById(R.id.textViewExample), requireView().findViewById(R.id.ConfigTitle),
            requireView().findViewById(R.id.textView5), requireView().findViewById(R.id.textCredits),
            requireView().findViewById(R.id.textView7), requireView().findViewById(R.id.textView6), requireView().findViewById(R.id.textView8),)
    }
}
