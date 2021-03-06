/*
Copyright 2012 Adam Bien, adam-bien.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.lightview.presentation.dashboard;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.collections.MapChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.lightview.presentation.applications.ApplicationsView;
import org.lightview.presentation.dashboard.toolbar.ToolbarView;
import org.lightview.presenter.ConnectionPoolBindings;
import org.lightview.view.Browser;
import org.lightview.view.ConnectionPool;
import org.lightview.view.Escalations;
import org.lightview.view.Snapshot;
import org.lightview.view.SnapshotTable;
import org.lightview.view.Status;

/**
 * User: blog.adam-bien.com Date: 18.11.11 Time: 17:19
 */
public class DashboardView {

    DashboardPresenter dashboardPresenter;
    Stage stage;
    private Browser browser;
    private Snapshot heap;
    private Snapshot threadCount;
    private Snapshot busyThread;
    private Snapshot queuedConnections;
    private Snapshot commitCount;
    private Snapshot rollbackCount;
    private Snapshot totalErrors;
    private Snapshot activeSessions;
    private Snapshot expiredSessions;
    private Snapshot peakThreadCount;
    private Snapshot successfulTXPerf;
    private Snapshot failedTXPerf;
    private SplitPane vertical;
    private ApplicationsView applicationsView;
    private ToolbarView toolbarView;
    private TabPane tabPane;
    private Status status;

    private Escalations escalations;
    private Scene scene;

    public DashboardView(Stage stage, DashboardPresenter dashboardPresenter) {
        this.dashboardPresenter = dashboardPresenter;
        this.stage = stage;
        this.tabPane = new TabPane();
        this.createViews();
        this.bind();
        this.open();
    }

    public void open() {
        this.toolbarView = new ToolbarView();
        ToolBar toolbar = (ToolBar) toolbarView.getViewWithoutRootContainer();

        BorderPane pane = new BorderPane();
        pane.setTop(toolbar);
        pane.setCenter(this.vertical);
        this.scene = new Scene(pane);
        scene.getStylesheets().add(this.getClass().getResource("lightview.css").toExternalForm());
        stage.setFullScreen(false);
        stage.setScene(scene);
        stage.show();
    }

    private void createViews() {
        this.applicationsView = new ApplicationsView();
        this.vertical = new SplitPane();
        this.vertical.setOrientation(Orientation.VERTICAL);
        HBox threadsAndMemory = new HBox();
        VBox paranormal = new VBox();
        HBox paranormalContent = new HBox();
        HBox transactions = new HBox();
        HBox web = new HBox();
        HBox performance = new HBox();

        String hBoxClass = "boxSpacing";
        this.vertical.getStyleClass().add(hBoxClass);
        threadsAndMemory.getStyleClass().add(hBoxClass);
        paranormalContent.getStyleClass().add(hBoxClass);
        transactions.getStyleClass().add(hBoxClass);
        web.getStyleClass().add(hBoxClass);

        instantiateViews();

        threadsAndMemory.getChildren().addAll(this.heap.view(), this.threadCount.view(), this.peakThreadCount.view());
        transactions.getChildren().addAll(this.commitCount.view(), this.rollbackCount.view());
        paranormalContent.getChildren().addAll(this.queuedConnections.view(), this.totalErrors.view(), this.busyThread.view());
        paranormal.getChildren().addAll(paranormalContent, this.status.view());
        performance.getChildren().addAll(this.successfulTXPerf.view());
        performance.getChildren().addAll(this.failedTXPerf.view());
        web.getChildren().addAll(this.activeSessions.view());
        web.getChildren().addAll(this.expiredSessions.view());
        Tab threadsAndMemoryTab = createTab(threadsAndMemory, "Threads And Memory");
        Tab transactionsTab = createTab(transactions, "Transactions");
        Tab paranormalTab = createTab(paranormal, "Paranormal Activity");
        Tab performanceTab = createTab(performance, "Performance");
        Tab webTab = createTab(web, "Web");
        this.tabPane.getTabs().addAll(threadsAndMemoryTab, transactionsTab, paranormalTab, performanceTab, webTab);
        this.vertical.getItems().addAll(this.tabPane, this.applicationsView.getView(), this.escalations.view());
        this.vertical.setDividerPositions(0.3, 0.7, 0.9);
    }

    private void instantiateViews() {
        this.browser = new Browser();
        ReadOnlyLongProperty id = this.dashboardPresenter.getId();
        this.heap = new Snapshot(id, "Heap Size", "Used Heap");
        this.threadCount = new Snapshot(id, "Thread Count", "Threads");
        this.peakThreadCount = new Snapshot(id, "Peak Thread Count", "Threads");
        this.busyThread = new Snapshot(id, "Busy Thread Count", "Threads");
        this.commitCount = new Snapshot(id, "TX Commit", "#");
        this.rollbackCount = new Snapshot(id, "TX Rollback", "#");
        this.totalErrors = new Snapshot(id, "Errors", "#");
        this.queuedConnections = new Snapshot(id, "Queued Connections", "Connections");
        this.activeSessions = new Snapshot(id, "HTTP Sessions", "#");
        this.expiredSessions = new Snapshot(id, "Expired Sessions", "#");
        this.successfulTXPerf = new Snapshot(id, "Commits Per Second", "#");
        this.failedTXPerf = new Snapshot(id, "Rollbacks Per Second", "#");
        final Node liveStream = new SnapshotTable(this.dashboardPresenter.getSnapshots()).createTable();
        this.escalations = new Escalations(liveStream, this.dashboardPresenter.getEscalationsPresenterBindings());
        this.status = new Status(this.dashboardPresenter.getDeadlockedThreads());
    }

    private void bind() {
        this.heap.value().bind(this.dashboardPresenter.getUsedHeapSizeInMB());

        this.threadCount.value().bind(this.dashboardPresenter.getThreadCount());
        this.busyThread.value().bind(this.dashboardPresenter.getBusyThreads());
        this.peakThreadCount.value().bind(this.dashboardPresenter.getPeakThreadCount());

        this.commitCount.value().bind(this.dashboardPresenter.getCommitCount());
        this.rollbackCount.value().bind(this.dashboardPresenter.getRollbackCount());

        this.queuedConnections.value().bind(this.dashboardPresenter.getQueuedConnections());
        this.totalErrors.value().bind(this.dashboardPresenter.getTotalErrors());
        this.activeSessions.value().bind(this.dashboardPresenter.getActiveSessions());
        this.expiredSessions.value().bind(this.dashboardPresenter.getExpiredSessions());
        this.successfulTXPerf.value().bind(this.dashboardPresenter.getCommitsPerSecond());
        this.failedTXPerf.value().bind(this.dashboardPresenter.getRollbacksPerSecond());

        this.dashboardPresenter.getPools().addListener(new MapChangeListener<String, ConnectionPoolBindings>() {
            public void onChanged(Change<? extends String, ? extends ConnectionPoolBindings> change) {
                ConnectionPoolBindings valueAdded = change.getValueAdded();
                if (valueAdded != null) {
                    createPoolTab(valueAdded);
                }
            }
        });
    }

    private Tab createTab(Node content, String caption) {
        Tab tab = new Tab();
        tab.setContent(content);
        tab.setText(caption);
        return tab;
    }

    void createPoolTab(ConnectionPoolBindings valueAdded) {
        ReadOnlyLongProperty id = this.dashboardPresenter.getId();
        String jndiName = valueAdded.getJndiName().get();
        ConnectionPool connectionPool = new ConnectionPool(id, valueAdded);
        Node view = connectionPool.view();
        Tab tab = createTab(view, "Resource: " + jndiName);
        this.tabPane.getTabs().add(tab);
    }
}
