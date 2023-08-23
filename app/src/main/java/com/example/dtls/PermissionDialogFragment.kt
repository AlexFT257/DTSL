
import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.dtls.R

class PermissionDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val positiveButton = view.findViewById<Button>(R.id.positiveButton)
        val negativeButton = view.findViewById<Button>(R.id.negativeButton)

        positiveButton.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
            dismiss()
        }

        negativeButton.setOnClickListener {
            dismiss()

        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 123
    }
}
