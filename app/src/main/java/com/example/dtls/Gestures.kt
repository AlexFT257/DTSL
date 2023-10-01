import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dtls.R
import android.content.Context
import android.content.SharedPreferences

class Gestures : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gestures, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        // List of alphabets from A to Z
        val alphabetList = ('A'..'Z').map { it.toString() }

        // List of image resources (Assuming you have R.drawable.image_a, R.drawable.image_b, etc.)
        val imageResources = ('A'..'Z').map { letter ->
            val resourceName = "${letter.toLowerCase()}"
            resources.getIdentifier(resourceName, "drawable", requireContext().packageName)
        }

        // Get SharedPreferences
        val sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Configuring RecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        val adapter = MyAdapter(alphabetList, imageResources, sharedPreferences)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Set the RecyclerView in the adapter to enable smooth scrolling
        adapter.setRecyclerView(recyclerView)

        return view
    }
}
