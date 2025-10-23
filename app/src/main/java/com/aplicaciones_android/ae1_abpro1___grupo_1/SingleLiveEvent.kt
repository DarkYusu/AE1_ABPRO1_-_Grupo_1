package com.aplicaciones_android.ae1_abpro1___grupo_1

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Un LiveData que solo envía actualizaciones una vez a los observadores.
 * Útil para eventos como navegación o mensajes de Snackbar.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, Observer { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    /**
     * Para casos donde T es Void, se puede llamar a call() en vez de setValue(null)
     */
    @MainThread
    fun call() {
        value = null
    }
}

