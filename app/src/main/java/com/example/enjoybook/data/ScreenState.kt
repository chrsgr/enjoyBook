package com.example.enjoybook.data

sealed class ScreenState {
    object Home : ScreenState()
    object Search : ScreenState()
    data class FilteredBooks(val categories: String) : ScreenState()
    data class QueryResults(val query: String) : ScreenState()
    object Add : ScreenState()
    object Favourite : ScreenState()
    object Book : ScreenState()

}