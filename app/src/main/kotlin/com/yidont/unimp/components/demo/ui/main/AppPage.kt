package com.yidont.unimp.components.demo.ui.main

import androidx.compose.runtime.Composable
import com.yidont.unimp.components.demo.LoadingBox
import com.yidont.unimp.components.demo.MainViewModel
import com.yidont.unimp.components.demo.ui.theme.AppTheme


@Composable
fun AppPage(viewModel: MainViewModel) {
    AppTheme {
        val state = viewModel.state
        MainPage(viewModel)
        if (state.loading) {
            LoadingBox(msg = state.loadText)
        }
    }
}