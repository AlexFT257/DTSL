import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.dtls.R

// Interfaz para escuchar cambios en el tamaño de fuente
interface FontSizeChangeListener {
    fun onFontSizeChanged(fontSize: Int)
}

class FontSettingsDialog(
    private val sharedPreferences: SharedPreferences,
    private val fontSizeChangeListener: FontSizeChangeListener // Agrega el parámetro al constructor
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.font_settings_dialog, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // Recuperar el tamaño de fuente actual desde las preferencias compartidas
        val currentFontSize = sharedPreferences.getInt(KEY_FONT_SIZE, MEDIUM_FONT_SIZE)

        // Marcar el botón de opción correspondiente según el tamaño de fuente actual
        when (currentFontSize) {
            SMALL_FONT_SIZE -> radioGroup.check(R.id.radioSmall)
            MEDIUM_FONT_SIZE -> radioGroup.check(R.id.radioMedium)
            LARGE_FONT_SIZE -> radioGroup.check(R.id.radioLarge)
        }

        btnSave.setOnClickListener {
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
            val newFontSize = when (selectedRadioButtonId) {
                R.id.radioSmall -> SMALL_FONT_SIZE
                R.id.radioMedium -> MEDIUM_FONT_SIZE
                R.id.radioLarge -> LARGE_FONT_SIZE
                else -> MEDIUM_FONT_SIZE
            }

            // Guardar el nuevo tamaño de fuente en las preferencias compartidas
            saveFontSizeToPreferences(newFontSize)

            // Notificar al listener sobre el cambio en el tamaño de fuente
            fontSizeChangeListener.onFontSizeChanged(newFontSize)

            // Cerrar el diálogo
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }

    private fun saveFontSizeToPreferences(fontSize: Int) {
        sharedPreferences.edit().putInt(KEY_FONT_SIZE, fontSize).apply()
    }

    companion object {
        const val KEY_FONT_SIZE = "fontSize"
        const val SMALL_FONT_SIZE = 14
        const val MEDIUM_FONT_SIZE = 18
        const val LARGE_FONT_SIZE = 24
    }
}
