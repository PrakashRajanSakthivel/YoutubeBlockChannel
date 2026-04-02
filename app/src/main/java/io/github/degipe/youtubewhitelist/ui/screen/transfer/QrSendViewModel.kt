package io.github.degipe.youtubewhitelist.ui.screen.transfer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.repository.ParentAccountRepository
import io.github.degipe.youtubewhitelist.core.export.ExportImportService
import io.github.degipe.youtubewhitelist.core.export.transfer.QrTransferClient
import io.github.degipe.youtubewhitelist.core.export.transfer.QrTransferServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QrSendUiState {
    /** Camera is active and scanning */
    data object Scanning : QrSendUiState()
    /** QR has been decoded; ask user to confirm before sending */
    data class Confirming(val ip: String, val port: Int, val token: String) : QrSendUiState()
    /** Exporting + sending in progress */
    data object Sending : QrSendUiState()
    /** Transfer completed successfully */
    data object Success : QrSendUiState()
    /** Something went wrong */
    data class Error(val message: String) : QrSendUiState()
}

@HiltViewModel
class QrSendViewModel @Inject constructor(
    private val exportImportService: ExportImportService,
    private val parentAccountRepository: ParentAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrSendUiState>(QrSendUiState.Scanning)
    val uiState: StateFlow<QrSendUiState> = _uiState.asStateFlow()

    /** Decode `ywl://transfer?ip=…&port=…&token=…` from camera */
    fun onQrScanned(content: String) {
        if (_uiState.value !is QrSendUiState.Scanning) return
        try {
            val uri = Uri.parse(content)
            if (uri.scheme != "ywl" || uri.host != "transfer") return
            val ip = uri.getQueryParameter("ip") ?: return
            val port = uri.getQueryParameter("port")?.toIntOrNull() ?: QrTransferServer.PORT
            val token = uri.getQueryParameter("token") ?: return
            _uiState.value = QrSendUiState.Confirming(ip, port, token)
        } catch (_: Exception) {
            // Not a valid transfer QR — keep scanning
        }
    }

    fun onConfirmSend() {
        val confirming = _uiState.value as? QrSendUiState.Confirming ?: return
        _uiState.value = QrSendUiState.Sending

        viewModelScope.launch {
            val account = parentAccountRepository.getAccount().first()
            if (account == null) {
                _uiState.value = QrSendUiState.Error("No parent account found. Please sign in first.")
                return@launch
            }

            val json = when (val result = exportImportService.exportToJson(account.id)) {
                is AppResult.Success -> result.data
                is AppResult.Error -> {
                    _uiState.value = QrSendUiState.Error("Export failed: ${result.message}")
                    return@launch
                }
            }

            val ok = QrTransferClient.send(confirming.ip, confirming.port, confirming.token, json)
            _uiState.value = if (ok) QrSendUiState.Success
            else QrSendUiState.Error("Could not reach the TV. Make sure both devices are on the same Wi-Fi network.")
        }
    }

    fun onCancelConfirm() {
        _uiState.value = QrSendUiState.Scanning
    }

    fun retry() {
        _uiState.value = QrSendUiState.Scanning
    }
}
