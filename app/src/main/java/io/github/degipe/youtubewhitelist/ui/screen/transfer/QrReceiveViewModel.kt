package io.github.degipe.youtubewhitelist.ui.screen.transfer

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.repository.ParentAccountRepository
import io.github.degipe.youtubewhitelist.core.export.ExportImportService
import io.github.degipe.youtubewhitelist.core.export.ImportResult
import io.github.degipe.youtubewhitelist.core.export.ImportStrategy
import io.github.degipe.youtubewhitelist.core.export.transfer.QrCodeGenerator
import io.github.degipe.youtubewhitelist.core.export.transfer.QrTransferServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject

sealed class QrReceiveUiState {
    data object Loading : QrReceiveUiState()
    data class Ready(val qrBitmap: Bitmap) : QrReceiveUiState()
    data object Receiving : QrReceiveUiState()
    data object Importing : QrReceiveUiState()
    data class Success(val result: ImportResult) : QrReceiveUiState()
    data class Error(val message: String) : QrReceiveUiState()
}

@HiltViewModel
class QrReceiveViewModel @Inject constructor(
    private val exportImportService: ExportImportService,
    private val parentAccountRepository: ParentAccountRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrReceiveUiState>(QrReceiveUiState.Loading)
    val uiState: StateFlow<QrReceiveUiState> = _uiState.asStateFlow()

    private var server: QrTransferServer? = null

    init {
        startServer()
    }

    private fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val ip = getLocalIpAddress()
            if (ip == null) {
                _uiState.value = QrReceiveUiState.Error("Could not determine Wi-Fi IP address. Make sure this device is connected to Wi-Fi.")
                return@launch
            }

            val token = QrTransferServer.generateToken()
            val qrContent = "ywl://transfer?ip=$ip&port=${QrTransferServer.PORT}&token=$token"

            val newServer = QrTransferServer(token) { json ->
                onDataReceived(json)
            }
            newServer.startServer()
            server = newServer

            val qrBitmap = withContext(Dispatchers.Default) {
                QrCodeGenerator.generate(qrContent, size = 512)
            }
            _uiState.value = QrReceiveUiState.Ready(qrBitmap)
        }
    }

    private fun onDataReceived(json: String) {
        _uiState.value = QrReceiveUiState.Receiving
        server?.stopServer()
        server = null

        viewModelScope.launch {
            _uiState.value = QrReceiveUiState.Importing
            val account = parentAccountRepository.getAccount().first()
            if (account == null) {
                _uiState.value = QrReceiveUiState.Error("No parent account found. Please sign in first.")
                return@launch
            }

            when (val result = exportImportService.importFromJson(account.id, json, ImportStrategy.MERGE)) {
                is AppResult.Success -> _uiState.value = QrReceiveUiState.Success(result.data)
                is AppResult.Error -> _uiState.value = QrReceiveUiState.Error(result.message ?: "Import failed")
            }
        }
    }

    fun retry() {
        server?.stopServer()
        server = null
        _uiState.value = QrReceiveUiState.Loading
        startServer()
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ipInt = wifiInfo.ipAddress
        if (ipInt == 0) return null
        return InetAddress.getByAddress(
            byteArrayOf(
                (ipInt and 0xFF).toByte(),
                (ipInt shr 8 and 0xFF).toByte(),
                (ipInt shr 16 and 0xFF).toByte(),
                (ipInt shr 24 and 0xFF).toByte()
            )
        ).hostAddress
    }

    override fun onCleared() {
        super.onCleared()
        server?.stopServer()
        server = null
    }
}
