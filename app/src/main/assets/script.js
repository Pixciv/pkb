document.addEventListener('DOMContentLoaded', () => {
    const mainHeader = document.getElementById('main-header');
    const selectionHeader = document.getElementById('selection-header');
    const selectIcon = document.getElementById('select-icon');
    const selectionCancel = document.getElementById('selection-cancel');
    const selectionAll = document.getElementById('selection-all');
    const selectionDelete = document.getElementById('selection-delete');
    const selectionMove = document.getElementById('selection-move');
    const selectionShare = document.getElementById('selection-share');
    const selectionCount = document.getElementById('selection-count');
    const bottomNav = document.getElementById('main-bottom-nav');

    const topTabs = document.querySelectorAll('.top-tabs .tab');
    const fileListContent = document.getElementById('file-list-content');
    const favoritesListContent = document.getElementById('favorites-list-content');
    const drawer = document.getElementById('drawer');
    const drawerToggle = document.getElementById('drawer-toggle');
    const backButton = document.getElementById('back-button');
    const headerTitle = document.getElementById('header-title');
    const overlay = document.getElementById('overlay');
    const darkModeToggle = document.getElementById('darkModeToggle');
    const sortIcon = document.getElementById('sort-icon');
    const sortPopup = document.getElementById('sort-popup');
    const sortOptions = document.querySelectorAll('.sort-option');
    const sortApplyButton = document.getElementById('sort-apply');
    const sortCancelButton = document.getElementById('sort-cancel');
    const searchIcon = document.getElementById('search-icon');
    const searchBar = document.getElementById('search-bar');
    const searchInput = document.getElementById('search-input');
    const searchBack = document.getElementById('search-back');
    const searchClear = document.getElementById('search-clear');
    const headerIcons = document.querySelector('#main-header .header-icons');
    const bottomNavItems = document.querySelectorAll('.bottom-nav .nav-item');
    const contentSections = document.querySelectorAll('.content-container .content');
    const fileMenuPopup = document.getElementById('file-menu-popup');

    let allFiles = [];
    let selectionMode = false;
    let selectedFiles = [];

    // Load files from localStorage
    function loadFiles() {
        const storedFiles = localStorage.getItem('allFiles');
        if (storedFiles) allFiles = JSON.parse(storedFiles);
    }
    function saveFiles() {
        localStorage.setItem('allFiles', JSON.stringify(allFiles));
    }
    loadFiles();

    // Dark mode setup
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (isDarkMode) {
        document.body.classList.add('dark-mode');
        darkModeToggle.checked = true;
    }

    // Navigation
    function showContent(navId) {
        contentSections.forEach(section => {
            section.classList.toggle('active', section.id === `${navId}-content`);
        });
        document.querySelector('.top-tabs').style.display = navId === 'all-files' ? 'flex' : 'none';
        headerTitle.textContent = navId === 'all-files' ? 'All PDF Reader' :
                                  navId === 'recent' ? 'Son Kullanılanlar' :
                                  navId === 'favorites' ? 'Favoriler' :
                                  navId === 'tools' ? 'Araçlar' : 'All PDF Reader';
        backButton.style.display = navId === 'all-files' ? 'none' : 'block';
        drawerToggle.style.display = navId === 'all-files' ? 'block' : 'none';
        sortIcon.style.display = 'block';
        searchIcon.style.display = 'block';
        exitSelectionMode();
        renderCurrentContent();
    }

    // Render files
    function renderContent(files, targetElement, fileTypeFilter = 'all', sortCriteria = 'date-desc', searchTerm = '') {
        targetElement.innerHTML = '';
        fileMenuPopup.style.display = 'none';

        let filesToRender = [...files];

        const typesMap = {
            'pdf': ['pdf'],
            'word': ['doc', 'docx'],
            'excel': ['xls', 'xlsx'],
            'ppt': ['ppt', 'pptx']
        };
        if (fileTypeFilter !== 'all') filesToRender = filesToRender.filter(f => typesMap[fileTypeFilter].includes(f.type));
        if (searchTerm) filesToRender = filesToRender.filter(f => f.name.toLowerCase().includes(searchTerm.toLowerCase()));

        const activeNavId = document.querySelector('.bottom-nav .nav-item.active').dataset.nav;
        if (activeNavId === 'favorites') filesToRender = filesToRender.filter(f => f.isFavorite);

        // Sorting
        filesToRender.sort((a,b) => {
            switch(sortCriteria){
                case 'date-desc': return new Date(b.date)-new Date(a.date);
                case 'date-asc': return new Date(a.date)-new Date(b.date);
                case 'name-asc': return a.name.localeCompare(b.name);
                case 'name-desc': return b.name.localeCompare(a.name);
                case 'size-asc': return parseFloat(a.size.replace(',', '.').replace(' kB','')) - parseFloat(b.size.replace(',', '.').replace(' kB',''));
                case 'size-desc': return parseFloat(b.size.replace(',', '.').replace(' kB','')) - parseFloat(a.size.replace(',', '.').replace(' kB',''));
                default: return 0;
            }
        });

        if (filesToRender.length === 0) {
            targetElement.innerHTML = `<div class="empty-state">
                <i class="fas fa-folder-open fa-3x"></i>
                <p>${searchTerm ? 'Aradığınız dosya bulunamadı.' : 'Henüz hiç dosyanız yok'}</p>
            </div>`;
            return;
        }

        filesToRender.forEach(item => {
            const fileItem = document.createElement('div');
            fileItem.className = 'file-item';
            if (selectionMode) fileItem.classList.add('selection-mode');

            let iconClass = 'fa-file';
            if(item.type==='pdf') iconClass='fa-file-pdf';
            else if(['doc','docx'].includes(item.type)) iconClass='fa-file-word';
            else if(['xls','xlsx'].includes(item.type)) iconClass='fa-file-excel';
            else if(['ppt','pptx'].includes(item.type)) iconClass='fa-file-powerpoint';

            const isSelected = selectedFiles.some(f=>f.name===item.name);

            fileItem.innerHTML = `
            <div class="file-item-wrapper">
                <input type="checkbox" class="file-checkbox" ${isSelected?'checked':''} data-filename="${item.name}">
                <div class="file-icon-container"><i class="fas ${iconClass} file-icon"></i></div>
                <div class="file-info">
                    <div class="file-name">${item.name}</div>
                    <div class="file-details">${item.date} · ${item.size}</div>
                </div>
                <div class="file-actions">
                    <i class="favorite-icon ${item.isFavorite?'fas fa-star':'far fa-star'}" data-filename="${item.name}"></i>
                    <i class="fas fa-ellipsis-v file-menu" data-filename="${item.name}"></i>
                </div>
            </div>`;

            const checkbox = fileItem.querySelector('.file-checkbox');
            const fileItemWrapper = fileItem.querySelector('.file-item-wrapper');

            fileItemWrapper.addEventListener('click', e=>{
                if(selectionMode && (e.target.classList.contains('file-item-wrapper') || e.target.closest('.file-info'))){
                    e.stopPropagation();
                    checkbox.checked=!checkbox.checked;
                    checkbox.dispatchEvent(new Event('change'));
                }
            });

            checkbox.addEventListener('change', e=>{
                const fileName=e.target.dataset.filename;
                const file = allFiles.find(f=>f.name===fileName);
                if(e.target.checked) selectedFiles.push(file);
                else selectedFiles = selectedFiles.filter(f=>f.name!==fileName);
                updateSelectionCount();
            });

            const favoriteIcon = fileItem.querySelector('.favorite-icon');
            favoriteIcon.addEventListener('click', e=>{
                e.stopPropagation();
                toggleFavorite(e.target.dataset.filename);
            });

            const fileMenuIcon = fileItem.querySelector('.file-menu');
            fileMenuIcon.addEventListener('click', e=>{
                e.stopPropagation();
                showFileMenu(e.target, e.target.dataset.filename);
            });

            targetElement.appendChild(fileItem);
        });
    }

    function updateSelectionCount(){
        selectionCount.textContent=`${selectedFiles.length} Dosya Seçildi`;
        selectionDelete.style.display = selectedFiles.length>0?'block':'none';
        selectionMove.style.display = selectedFiles.length>0?'block':'none';
        selectionShare.style.display = selectedFiles.length>0?'block':'none';
    }

    function toggleFavorite(fileName){
        const file = allFiles.find(f=>f.name===fileName);
        if(file){ file.isFavorite=!file.isFavorite; saveFiles(); renderCurrentContent(); }
    }

    function renameFile(oldName){
        const file = allFiles.find(f=>f.name===oldName);
        if(file){
            const newName = prompt("Dosyayı yeniden adlandır:", file.name);
            if(newName && newName!==file.name){ file.name=newName; saveFiles(); renderCurrentContent(); }
        }
    }

    function deleteFile(fileName){
        if(confirm(`${fileName} dosyasını silmek istediğinizden emin misiniz?`)){
            allFiles = allFiles.filter(f=>f.name!==fileName);
            saveFiles();
            renderCurrentContent();
        }
    }

    function deleteSelectedFiles(){
        if(selectedFiles.length>0 && confirm(`${selectedFiles.length} dosyayı silmek istediğinizden emin misiniz?`)){
            const selectedNames = selectedFiles.map(f=>f.name);
            allFiles = allFiles.filter(f=>!selectedNames.includes(f.name));
            saveFiles();
            exitSelectionMode();
        }
    }

    selectionDelete.addEventListener('click', deleteSelectedFiles);
    selectionMove.addEventListener('click', ()=>{ alert("Dosyalar taşınıyor..."); exitSelectionMode(); });
    selectionShare.addEventListener('click', ()=>{ alert("Dosyalar paylaşılıyor..."); exitSelectionMode(); });

    function showFileMenu(targetIcon, fileName){
        const rect = targetIcon.getBoundingClientRect();
        fileMenuPopup.style.top=`${rect.bottom+5}px`;
        fileMenuPopup.style.left=`${rect.left-150}px`;
        fileMenuPopup.style.display='block';
        fileMenuPopup.dataset.targetFile=fileName;

        const file = allFiles.find(f=>f.name===fileName);
        const favoriteMenuItem = fileMenuPopup.querySelector('.menu-favorite span');
        favoriteMenuItem.textContent = file.isFavorite?'Favorilerden Kaldır':'Favorilere Ekle';
        fileMenuPopup.querySelector('.menu-favorite i').className = file.isFavorite?'fas fa-star':'far fa-star';
    }

    fileMenuPopup.addEventListener('click', e=>{
        const action = e.target.closest('li').dataset.action;
        const fileName = fileMenuPopup.dataset.targetFile;
        if(action==='toggle-favorite') toggleFavorite(fileName);
        else if(action==='rename') renameFile(fileName);
        else if(action==='delete') deleteFile(fileName);
        fileMenuPopup.style.display='none';
    });

    document.addEventListener('click', e=>{
        if(!fileMenuPopup.contains(e.target) && !e.target.classList.contains('file-menu')){
            fileMenuPopup.style.display='none';
        }
    });

    function renderCurrentContent(){
        const activeNav = document.querySelector('.bottom-nav .nav-item.active').dataset.nav;
        let targetElement = activeNav==='favorites'?favoritesListContent:fileListContent;
        const activeTab = document.querySelector('.top-tabs .tab.active');
        const activeTabType = activeTab ? activeTab.dataset.tab:'all';
        const currentSort = document.querySelector('.sort-option.active').dataset.sort;
        const currentSearchTerm = searchInput.value;
        renderContent(allFiles, targetElement, activeTabType, currentSort, currentSearchTerm);
    }

    function enterSelectionMode(){
        selectionMode=true;
        selectedFiles=[];
        mainHeader.style.display='none';
        selectionHeader.style.display='flex';
        bottomNav.style.display='none';
        document.body.classList.add('selection-mode');
        renderCurrentContent();
        updateSelectionCount();
    }

    function exitSelectionMode(){
        selectionMode=false;
        selectedFiles=[];
        mainHeader.style.display='flex';
        selectionHeader.style.display='none';
        bottomNav.style.display='flex';
        document.body.classList.remove('selection-mode');
        renderCurrentContent();
    }

    selectIcon.addEventListener('click', enterSelectionMode);
    selectionCancel.addEventListener('click', exitSelectionMode);

    selectionAll.addEventListener('click', ()=>{
        const allCheckboxes=document.querySelectorAll('.file-checkbox');
        const allSelected = selectedFiles.length===allCheckboxes.length;
        selectedFiles=[];
        allCheckboxes.forEach(checkbox=>{
            checkbox.checked = !allSelected;
            if(!allSelected){
                const fileName = checkbox.dataset.filename;
                const file = allFiles.find(f=>f.name===fileName);
                if(file) selectedFiles.push(file);
            }
        });
        updateSelectionCount();
    });

    searchIcon.addEventListener('click', ()=>{
        headerTitle.style.display='none';
        headerIcons.style.display='none';
        drawerToggle.style.display='none';
        backButton.style.display='none';
        searchBar.style.display='flex';
        searchInput.focus();
    });

    searchBack.addEventListener('click', ()=>{
        headerTitle.style.display='block';
        headerIcons.style.display='flex';
        drawerToggle.style.display='block';
        backButton.style.display='none';
        searchBar.style.display='none';
        searchInput.value='';
        renderCurrentContent();
    });

    searchClear.addEventListener('click', ()=>{
        searchInput.value='';
        searchInput.focus();
        renderCurrentContent();
    });

    searchInput.addEventListener('input', renderCurrentContent);

    sortIcon.addEventListener('click', ()=> sortPopup.style.display='flex');
    sortCancelButton.addEventListener('click', ()=> sortPopup.style.display='none');
    sortOptions.forEach(option=>{
        option.addEventListener('click', ()=>{
            sortOptions.forEach(opt=>opt.classList.remove('active'));
            option.classList.add('active');
        });
    });
    sortApplyButton.addEventListener('click', ()=>{
        sortPopup.style.display='none';
        renderCurrentContent();
    });

    topTabs.forEach(tab=>{
        tab.addEventListener('click', ()=>{
            topTabs.forEach(t=>t.classList.remove('active'));
            tab.classList.add('active');
            renderCurrentContent();
        });
    });

    function toggleDrawer(open){
        drawer.classList.toggle('active', open);
        overlay.classList.toggle('active', open);
    }
    drawerToggle.addEventListener('click', ()=>toggleDrawer(true));
    overlay.addEventListener('click', ()=>toggleDrawer(false));

    backButton.addEventListener('click', ()=>{
        bottomNavItems.forEach(item=>item.classList.remove('active'));
        document.querySelector('.nav-item[data-nav="all-files"]').classList.add('active');
        document.querySelector('.top-tabs .tab[data-tab="all"]').classList.add('active');
        showContent('all-files');
    });

    bottomNavItems.forEach(item=>{
        item.addEventListener('click', ()=>{
            const navId=item.dataset.nav;
            bottomNavItems.forEach(nav=>nav.classList.remove('active'));
            item.classList.add('active');
            showContent(navId);
        });
    });

    // Swipe drawer
    let touchStartX=0;
    document.addEventListener('touchstart', e=>{ touchStartX=e.touches[0].clientX; });
    document.addEventListener('touchend', e=>{
        const deltaX = e.changedTouches[0].clientX - touchStartX;
        if(deltaX<-50 && drawer.classList.contains('active')) toggleDrawer(false);
        else if(deltaX>50 && !drawer.classList.contains('active')) toggleDrawer(true);
    });

    renderCurrentContent();
});
