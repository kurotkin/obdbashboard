package com.kurotkin.testobd.ui.main

import android.graphics.Typeface
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.kurotkin.testobd.ObdProvider
import com.kurotkin.testobd.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class PanelFragment : Fragment() {

    companion object {
        fun newInstance() = PanelFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var speedTextView: TextView
    private lateinit var loadTextView: TextView
    private lateinit var fuelTextView: TextView
    private lateinit var timeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        speedTextView = view?.findViewById(R.id.speed) as TextView
        loadTextView = view?.findViewById(R.id.load) as TextView
        fuelTextView = view?.findViewById(R.id.fuel) as TextView
        timeTextView = view?.findViewById(R.id.timestamp) as TextView

        val typeFont: Typeface = Typeface.createFromAsset(activity?.assets, "fonts/menlo-regular.ttf")
        speedTextView.typeface = typeFont
        loadTextView.typeface = typeFont
        fuelTextView.typeface = typeFont

        val adr = viewModel.bluetooth(context!!)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    run(it)
                }
            },{
                Toast.makeText(context, "Устройства не найдены", Toast.LENGTH_LONG).show()
            })
    }

    fun run(adr: String){
        val v = ObdProvider().bluetoothWork2(adr)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                timeTextView.text = "${viewModel.controlTime()} ms"
                speedTextView.text = it.speed.toString()
                loadTextView.text = it.load
                fuelTextView.text = it.fuel
            }, {
                timeTextView.text = "${it.message}"
            }, {

            }, {

            })
    }

}