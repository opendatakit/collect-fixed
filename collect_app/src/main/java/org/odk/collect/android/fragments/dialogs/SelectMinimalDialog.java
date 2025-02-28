package org.odk.collect.android.fragments.dialogs;

import static org.odk.collect.android.injection.DaggerUtils.getComponent;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import org.javarosa.core.model.data.helper.Selection;
import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.R;
import org.odk.collect.android.adapters.AbstractSelectListAdapter;
import org.odk.collect.android.databinding.SelectMinimalDialogLayoutBinding;
import org.odk.collect.android.fragments.viewmodels.SelectMinimalViewModel;
import org.odk.collect.android.widgets.utilities.ViewModelAudioPlayer;
import org.odk.collect.audioclips.AudioClipViewModel;
import org.odk.collect.material.MaterialFullScreenDialogFragment;

import java.util.List;

public abstract class SelectMinimalDialog extends MaterialFullScreenDialogFragment {

    private SelectMinimalDialogLayoutBinding binding;

    private boolean isFlex;
    private boolean isAutocomplete;

    protected SelectMinimalViewModel viewModel;
    protected SelectMinimalDialogListener listener;
    protected AbstractSelectListAdapter adapter;

    public interface SelectMinimalDialogListener {
        void updateSelectedItems(List<Selection> items);
    }

    public SelectMinimalDialog() {
    }

    public SelectMinimalDialog(boolean isFlex, boolean isAutoComplete) {
        this.isFlex = isFlex;
        this.isAutocomplete = isAutoComplete;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        getComponent(context).inject(this);
        if (context instanceof SelectMinimalDialogListener) {
            listener = (SelectMinimalDialogListener) context;
        }
        viewModel = new ViewModelProvider(this, new SelectMinimalViewModel.Factory(adapter, isFlex, isAutocomplete)).get(SelectMinimalViewModel.class);
        if (viewModel.getSelectListAdapter() == null) {
            dismiss();
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SelectMinimalDialogLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        initToolbar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.getSelectListAdapter().getAudioPlayer().stop();
        binding = null;
    }

    @Override
    protected void onCloseClicked() {
        closeDialogAndSaveAnswers();
    }

    @Override
    protected void onBackPressed() {
        closeDialogAndSaveAnswers();
    }

    protected void closeDialogAndSaveAnswers() {
        viewModel.getSelectListAdapter().getFilter().filter("");
        if (viewModel.getSelectListAdapter().hasAnswerChanged()) {
            listener.updateSelectedItems(viewModel.getSelectListAdapter().getSelectedItems());
        }
        dismiss();
    }

    @Nullable
    @Override
    protected Toolbar getToolbar() {
        return getView().findViewById(org.odk.collect.androidshared.R.id.toolbar);
    }

    private void initToolbar() {
        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);

        if (viewModel.isAutoComplete()) {
            initSearchBar();
        }
    }

    private void initSearchBar() {
        getToolbar().inflateMenu(R.menu.select_minimal_dialog_menu);

        SearchView searchView = (SearchView) getToolbar().getMenu().findItem(R.id.menu_filter).getActionView();
        searchView.setQueryHint(getResources().getString(org.odk.collect.strings.R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.onActionViewExpanded();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.getSelectListAdapter().getFilter().filter(newText);
                return false;
            }
        });
    }

    private void initRecyclerView() {
        viewModel.getSelectListAdapter().setContext(getActivity());
        viewModel.getSelectListAdapter().setAudioPlayer(new ViewModelAudioPlayer(new ViewModelProvider(requireActivity()).get(AudioClipViewModel.class), getViewLifecycleOwner()));
        binding.choicesRecyclerView.initRecyclerView(viewModel.getSelectListAdapter(), viewModel.isFlex());
    }

    public void setListener(SelectMinimalDialogListener listener) {
        this.listener = listener;
    }
}
