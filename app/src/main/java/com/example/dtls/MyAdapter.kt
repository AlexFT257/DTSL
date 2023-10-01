import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.dtls.R

class MyAdapter(
    private val dataList: List<String>,
    private val imageResources: List<Int>,
    private val sharedPreferences: SharedPreferences
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    private var expandedPosition: Int = -1
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = dataList[position]
        holder.bind(item, imageResources[position], sharedPreferences, position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.itemContainer)
        private val textView: TextView = itemView.findViewById(R.id.itemTextView)
        private val additionalImageView: ImageView = itemView.findViewById(R.id.additionalImageView)

        fun bind(item: String, imageResource: Int, sharedPreferences: SharedPreferences, position: Int) {
            textView.text = item
            val isExpanded = position == expandedPosition
            val fontSize = sharedPreferences.getInt(FontSettingsDialog.KEY_FONT_SIZE, FontSettingsDialog.MEDIUM_FONT_SIZE)

            // Ajustar el tamaño de fuente de itemTextView en función de la configuración
            when (fontSize) {
                FontSettingsDialog.SMALL_FONT_SIZE -> textView.textSize = 14f
                FontSettingsDialog.MEDIUM_FONT_SIZE -> textView.textSize = 18f
                FontSettingsDialog.LARGE_FONT_SIZE -> textView.textSize = 24f
            }

            container.setOnClickListener {
                if (isExpanded) {
                    expandedPosition = -1
                } else {
                    expandedPosition = position
                }
                notifyDataSetChanged()

                // Mueve el elemento abierto hacia arriba en la pantalla
                if (expandedPosition != -1) {
                    recyclerView?.smoothScrollToPosition(expandedPosition)
                }
            }

            val scalePercent = 80  // Escala deseada en porcentaje
            val targetWidth = 1080 * scalePercent / 100
            val targetHeight = 1920 * scalePercent / 100

            if (isExpanded) {
                additionalImageView.visibility = View.VISIBLE

                // Usar Glide para cargar la imagen desde los recursos
                Glide.with(itemView)
                    .load(imageResource) // Cargar la imagen desde los recursos
                    .apply(RequestOptions()
                        .override(targetWidth, targetHeight) // Establece el tamaño deseado en píxeles
                        .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(additionalImageView)
            } else {
                // Si no está expandido, ocultar la imagen adicional
                additionalImageView.visibility = View.GONE
            }
        }
    }

    // Método para establecer la referencia al RecyclerView
    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }
}
