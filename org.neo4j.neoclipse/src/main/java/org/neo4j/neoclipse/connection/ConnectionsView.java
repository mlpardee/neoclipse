package org.neo4j.neoclipse.connection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;
import org.neo4j.neoclipse.Activator;
import org.neo4j.neoclipse.connection.actions.NewAliasAction;
import org.neo4j.neoclipse.connection.actions.NewEditorAction;

public class ConnectionsView extends ViewPart implements ConnectionListener
{

    public static final String ID = ConnectionsView.class.getCanonicalName();
    private static final Set<Alias> EMPTY_ALIASES = new HashSet<Alias>();

    private TreeViewer _treeViewer;

    private Clipboard clipboard;

    public ConnectionsView()
    {
        super();
        Activator.getDefault().setConnectionsView( this );
    }

    @Override
    public void createPartControl( Composite parent )
    {
        Activator.getDefault().getAliasManager().addListener( this );

        _treeViewer = new TreeViewer( parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
        getSite().setSelectionProvider( _treeViewer );

        IToolBarManager toolBarMgr = getViewSite().getActionBars().getToolBarManager();
        toolBarMgr.add( new NewAliasAction() );
        toolBarMgr.add( new NewEditorAction() );

        _treeViewer.setUseHashlookup( true );
        _treeViewer.setContentProvider( new ConnectionTreeContentProvider() );
        _treeViewer.setLabelProvider( new ConnectionTreeLabelProvider() );
        _treeViewer.setInput( Activator.getDefault().getAliasManager() );

        // doubleclick on alias opens session
        _treeViewer.addDoubleClickListener( new IDoubleClickListener()
        {
            @Override
            public void doubleClick( DoubleClickEvent event )
            {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if ( selection != null )
                {
                    Object selected = selection.getFirstElement();
                    if ( selected instanceof Alias )
                    {
                        Alias alias = (Alias) selection.getFirstElement();
                        // TODO Execute StartAction
                    }
                }
            }
        } );

        _treeViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged( SelectionChangedEvent event )
            {
                refreshToolbar();
            }
        } );

        // add context menu
        final ConnectionTreeActionGroup actionGroup = new ConnectionTreeActionGroup();
        MenuManager menuManager = new MenuManager( "ConnectionTreeContextMenu" );
        menuManager.setRemoveAllWhenShown( true );
        Menu contextMenu = menuManager.createContextMenu( _treeViewer.getTree() );
        _treeViewer.getTree().setMenu( contextMenu );

        menuManager.addMenuListener( new IMenuListener()
        {
            @Override
            public void menuAboutToShow( IMenuManager manager )
            {
                actionGroup.fillContextMenu( manager );
            }
        } );
        _treeViewer.setAutoExpandLevel( 2 );

        parent.layout();

    }

    @Override
    public void dispose()
    {
        if ( clipboard != null )
        {
            clipboard.dispose();
            clipboard = null;
        }
        super.dispose();
    }

    public TreeViewer getTreeViewer()
    {
        return _treeViewer;
    }

    public void refresh()
    {
        Display.getDefault().asyncExec( new Runnable()
        {
            @Override
            public void run()
            {
                if ( !_treeViewer.getTree().isDisposed() )
                {
                    _treeViewer.refresh();
                }
            }
        } );
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#setFocus()
     */
    @Override
    public void setFocus()
    {

    }

    /**
     * Helper method which returns the first element of a set, or null if the
     * set is empty (or if the set is null)
     * 
     * @param set the set to look into (may be null)
     * @return
     */
    private <T> T getFirstOf( Collection<T> set )
    {
        if ( set == null )
        {
            return null;
        }
        Iterator<T> iter = set.iterator();
        if ( iter.hasNext() )
        {
            return iter.next();
        }
        return null;
    }

    /**
     * @return the clipboard
     */
    public Clipboard getClipboard()
    {
        if ( clipboard == null )
        {
            clipboard = new Clipboard( getSite().getShell().getDisplay() );
        }
        return clipboard;
    }

    /**
     * @param clipboard the clipboard to set
     */
    public void setClipboard( Clipboard clipboard )
    {
        this.clipboard = clipboard;
    }

    @Override
    public void modelChanged()
    {

        getSite().getShell().getDisplay().asyncExec( new Runnable()
        {
            @Override
            public void run()
            {
                if ( !_treeViewer.getTree().isDisposed() )
                {
                    _treeViewer.refresh();
                    refreshToolbar();
                }
            }
        } );
    }

    private void refreshToolbar()
    {
        IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
        IContributionItem[] items = toolbar.getItems();
        for ( IContributionItem item : items )
        {
            if ( item instanceof ActionContributionItem )
            {
                ActionContributionItem contrib = (ActionContributionItem) item;
                IAction contribAction = contrib.getAction();
                if ( contribAction instanceof AbstractConnectionTreeAction )
                {
                    AbstractConnectionTreeAction action = (AbstractConnectionTreeAction) contribAction;
                    action.setEnabled( action.isAvailable() );
                }
            }
        }
    }

    Object[] getSelected()
    {
        IStructuredSelection selection = (IStructuredSelection) _treeViewer.getSelection();
        if ( selection == null )
        {
            return null;
        }
        Object[] result = selection.toArray();
        if ( result.length == 0 )
        {
            return null;
        }
        return result;
    }

    public void openNewEditor()
    {
        // TODO new Cypher Editor
    }

    public Set<Alias> getSelectedAliases()
    {
        IStructuredSelection selection = (IStructuredSelection) _treeViewer.getSelection();
        if ( selection == null )
        {
            return EMPTY_ALIASES;
        }

        LinkedHashSet<Alias> result = new LinkedHashSet<Alias>();
        Iterator<?> iter = selection.iterator();
        while ( iter.hasNext() )
        {
            Object obj = iter.next();
            if ( obj instanceof Alias )
            {
                result.add( (Alias) obj );
            }

        }

        return result;
    }

    public Alias getSelectedAlias()
    {
        return getFirstOf( getSelectedAliases() );
    }
}
