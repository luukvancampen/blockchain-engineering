package nl.tudelft.ipv8.android.demo.ui.blocks

import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_blocks.*
import kotlinx.android.synthetic.main.fragment_peers.recyclerView
import nl.tudelft.ipv8.android.demo.DemoCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.util.hexToBytes


open class BlocksFragment : BaseFragment() {
    private val adapter = ItemAdapter()

    private lateinit var publicKey: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        publicKey = getPublicKey()

        adapter.registerRenderer(BlockItemRenderer {
            it.isExpanded = !it.isExpanded
            adapter.notifyDataSetChanged()
        })

        crawlChain()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blocks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        updateView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.blocks_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_new_block -> {
                showNewBlockDialog()
               true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected open fun getPublicKey(): ByteArray {
        val args = BlocksFragmentArgs.fromBundle(arguments!!)
        return args.publicKey.hexToBytes()
    }

    private fun updateView() {
        val demoCommunity = getDemoCommunity()
        val blocks = demoCommunity.getChainByUser(publicKey)
        val items = blocks.map {
            BlockItem(it)
        }
        adapter.updateItems(items)
        imgNoBlocks.isVisible = items.isEmpty()
    }

    private fun showNewBlockDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("New Block")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Create") { dialog, which ->
            val message = input.text.toString()
            val demoCommunity = getDemoCommunity()
            demoCommunity.createProposalBlock(message, publicKey)
            updateView()
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun crawlChain() {
        val peer = getDemoCommunity().getPeerByPublicKeyBin(publicKey)
        if (peer != null) {
            val trustChainStore = getDemoCommunity().trustChainCommunity.database
            val lowestSeqNumUnknown = trustChainStore.getLowestSequenceNumberUnknown(publicKey)
            val latestBlockNum = (lowestSeqNumUnknown - 1).toInt()
            getDemoCommunity().crawlChain(peer, latestBlockNum)
        }
    }
}
