package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val isRealGps: Boolean,
    val addressEstimate: String
)

object LocationHelper {
    // Coordenadas Centrais de Apiaí - SP (Praça Alberto Dias / Centro)
    const val APIAI_DEFAULT_LAT = -24.5086
    const val APIAI_DEFAULT_LNG = -48.8465

    fun getCurrentCoordinates(context: Context): GpsCoordinates {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return GpsCoordinates(
                latitude = APIAI_DEFAULT_LAT,
                longitude = APIAI_DEFAULT_LNG,
                isRealGps = false,
                addressEstimate = "Apiaí - SP (Localização de Referência Centro)"
            )
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            return GpsCoordinates(
                latitude = APIAI_DEFAULT_LAT,
                longitude = APIAI_DEFAULT_LNG,
                isRealGps = false,
                addressEstimate = "Apiaí - SP (Centro)"
            )
        }

        val location: Location? = try {
            val gpsLocation = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
            gpsLocation ?: if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
        } catch (e: SecurityException) {
            null
        }

        return if (location != null) {
            GpsCoordinates(
                latitude = location.latitude,
                longitude = location.longitude,
                isRealGps = true,
                addressEstimate = "Lat: ${"%.4f".format(location.latitude)}, Lng: ${"%.4f".format(location.longitude)} (Apiaí - SP)"
            )
        } else {
            GpsCoordinates(
                latitude = APIAI_DEFAULT_LAT,
                longitude = APIAI_DEFAULT_LNG,
                isRealGps = false,
                addressEstimate = "Apiaí - SP (Estimativa por Antena/Centro)"
            )
        }
    }

    fun openDialer(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openWhatsAppEmergency(
        context: Context,
        phoneNumber: String,
        userName: String,
        lat: Double,
        lng: Double
    ) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val formattedNumber = if (cleanNumber.startsWith("55")) cleanNumber else "55$cleanNumber"
        val message = "🚨 *ALERTA SOS MAX SEG - RESGATE IMEDIATO*\n\n" +
                "Associado: *$userName*\n" +
                "Status: Socorro Acionado via Botão de Pânico 24h\n" +
                "Localização GPS: https://maps.google.com/?q=$lat,$lng\n" +
                "Cidade: Apiaí - SP\n" +
                "Central de Monitoramento Max Seg Apiaí 24h."
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=" + Uri.encode(message))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback
            openDialer(context, phoneNumber)
        }
    }
}
