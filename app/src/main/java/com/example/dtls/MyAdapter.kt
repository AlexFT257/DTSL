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
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    private var expandedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = dataList[position]
        holder.bind(item, imageResources[position], position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.itemContainer)
        private val textView: TextView = itemView.findViewById(R.id.itemTextView)
        private val additionalImageView: ImageView = itemView.findViewById(R.id.additionalImageView)

        fun bind(item: String, imageResource: Int, position: Int) {
            textView.text = "$item"
            val isExpanded = position == expandedPosition

            container.setOnClickListener {
                if (isExpanded) {
                    expandedPosition = -1
                } else {
                    expandedPosition = position
                }
                notifyDataSetChanged()

                // Mueve el elemento abierto hacia arriba en la pantalla
                if (expandedPosition != -1) {
                    recyclerView.smoothScrollToPosition(expandedPosition)
                }
            }

            additionalImageView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                val scalePercent = 80  // Escala deseada en porcentaje
                val targetWidth = 1080 * scalePercent / 100
                val targetHeight = 1920 * scalePercent / 100
                val requestOptions = RequestOptions()
                    .override(targetWidth, targetHeight)
                    .centerCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)

                Glide.with(itemView)
                    .load(imageResource)
                    .apply(requestOptions)
                    .into(additionalImageView)
            }
        }
    }
}
