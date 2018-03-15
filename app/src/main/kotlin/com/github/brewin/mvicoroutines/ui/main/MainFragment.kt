package com.github.brewin.mvicoroutines.ui.main


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.view.isVisible
import androidx.view.iterator
import com.github.brewin.mvicoroutines.NavigatorAction
import com.github.brewin.mvicoroutines.R
import com.github.brewin.mvicoroutines.data.GitHubApi
import com.github.brewin.mvicoroutines.data.Repository
import com.github.brewin.mvicoroutines.navigator
import com.github.brewin.mvicoroutines.ui.base.GenericListAdapter
import com.github.brewin.mvicoroutines.ui.base.UiRenderer
import com.github.brewin.mvicoroutines.ui.base.uiProvider
import kotlinx.android.synthetic.main.fragment_main.*
import org.jetbrains.anko.appcompat.v7.coroutines.onQueryTextListener
import org.jetbrains.anko.sdk23.coroutines.onClick
import timber.log.Timber

class MainFragment : Fragment(), UiRenderer<MainUiAction, MainUiResult, MainUiState> {

    override val ui by uiProvider { MainUi(Repository(GitHubApi.service)) }

    private val listAdapter by lazy {
        GenericListAdapter<Button, ReposItem>(R.layout.item_repo) { button, item ->
            val reposItem = ReposItem(item.name, item.url)
            button.text = item.name
            //FIXME: Opening item doesn't really change state. Just open here?
            button.onClick { ui.offerAction(MainUiAction.ItemClick(reposItem)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (item in menu.iterator()) when (item.itemId) {
            R.id.action_search -> {
                (item.actionView as SearchView).onQueryTextListener {
                    onQueryTextSubmit { query ->
                        query?.let { ui.offerActionWithProgress(MainUiAction.Search(it)) }
                        true
                    }
                }
            }
            R.id.action_refresh -> {
                item.setOnMenuItemClickListener {
                    ui.offerActionWithProgress(MainUiAction.Refresh)
                    true
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        swipeRefreshLayout.setOnRefreshListener { ui.offerActionWithProgress(MainUiAction.Refresh) }
        reposRecyclerView.adapter = listAdapter
        ui.startRendering(this)
    }

    override fun render(state: MainUiState) {
        Timber.d("State:\n$state")
        when (state) {
            is MainUiState.InProgress -> {
                emptyView.isVisible = false
                swipeRefreshLayout.isRefreshing = true
            }
            is MainUiState.Success -> {
                listAdapter.items = state.content
                swipeRefreshLayout.isRefreshing = false
            }
            is MainUiState.Failure -> {
                emptyView.text = state.error.message ?: "Unknown Error"
                emptyView.isVisible = true
                listAdapter.items = emptyList()
                swipeRefreshLayout.isRefreshing = false
            }
            is MainUiState.OpenUrl -> {
                navigator.offer(NavigatorAction.OpenUri(requireContext(), state.itemUrlToOpen))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ui.stopRendering()
    }
}
