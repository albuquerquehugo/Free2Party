package com.example.free2party.ui.screens.addfriend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddFriendScreen(viewModel: FriendViewModel = viewModel()) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Friends", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            label = { Text("Friend's Email") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )

        Button(
            onClick = { viewModel.findAndAddFriend() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isSearching
        ) {
            if (viewModel.isSearching) {
//                CircularProgressIndicator(size = 20.dp)
                CircularProgressIndicator()
            } else {
                Text("Search and Add Friend")
            }
        }

        if (viewModel.statusMessage.isNotEmpty()) {
            Text(viewModel.statusMessage, modifier = Modifier.padding(top = 16.dp))
        }
    }
}