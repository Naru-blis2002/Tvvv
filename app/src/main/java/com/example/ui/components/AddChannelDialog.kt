package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelDialog(
    onDismissRequest: () -> Unit,
    onChannelAdded: (name: String, country: String, url: String, category: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var isErrorUrl by remember { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && 
                      country.isNotBlank() && 
                      url.isNotBlank() && 
                      category.isNotBlank()

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_channel_dialog_container")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Añadir Canal IPTV",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 1. Channel name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Canal") },
                    placeholder = { Text("Ej. Telecentro HD") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_input_name")
                )

                // 2. Country input
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("País") },
                    placeholder = { Text("Ej. Argentina, Costa Rica, Perú") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_input_country")
                )

                // 3. Category input
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    placeholder = { Text("Ej. Noticias, Deportes, Música") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_input_category")
                )

                // 4. Stream URL Link input
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it 
                        isErrorUrl = it.isNotEmpty() && !it.startsWith("http://") && !it.startsWith("https://") && !it.startsWith("rtmp://")
                    },
                    label = { Text("Enlace IPTV (.m3u8 o .mp4)") },
                    placeholder = { Text("https://example.com/playlist.m3u8") },
                    singleLine = true,
                    isError = isErrorUrl,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("dialog_input_url"),
                    supportingText = {
                        if (isErrorUrl) {
                            Text(
                                text = "El enlace debe iniciar con http://, https://, o rtmp://",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp
                            )
                        }
                    }
                )

                // 5. Short description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (Opcional)") },
                    placeholder = { Text("Escribe una breve descripción del canal") },
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("dialog_input_description")
                )

                // Buttons controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isFormValid && !isErrorUrl) {
                                onChannelAdded(
                                    name.trim(),
                                    country.trim(),
                                    url.trim(),
                                    category.trim(),
                                    description.trim().ifEmpty { "Canal personalizado añadido por el usuario." }
                                )
                            }
                        },
                        enabled = isFormValid && !isErrorUrl,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_confirm_button")
                    ) {
                        Text("Añadir")
                    }
                }
            }
        }
    }
}
