document.addEventListener('DOMContentLoaded', () => {

    // ---------------------------
    // Elementler
    // ---------------------------
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
    const importFileItem = document.getElementById('import-file-item');

    let allFiles = [];
    let selectionMode = false;
    let selectedFiles = [];

    // ---------------------------
    // LocalStorage
    // ---------------------------
    function loadFiles() {
        const storedFiles = localStorage.getItem('allFiles');
        if (storedFiles) allFiles = JSON.parse(storedFiles);
    }
    function saveFiles() {
        localStorage.setItem('allFiles', JSON.stringify(allFiles));
    }
    loadFiles();

    // ---------------------------
    // Dark Mode
    // ---------------------------
    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (isDarkMode) {
        document.body.classList.add('dark-mode');
        darkModeToggle.checked = true;
    }
    darkModeToggle.addEventListener('change', ()=>{
        document.body.classList.toggle('dark-mode', darkModeToggle.checked);
        localStorage.setItem('darkMode', darkModeToggle.checked);
    });

    // ---------------------------
    // Drawer
    // ---------------------------
    function toggleDrawer(open){
        drawer.classList.toggle('active', open);
        overlay.classList.toggle('active', open);
    }
    drawerToggle.addEventListener('click', ()=>toggleDrawer(true));
    overlay.addEventListener('click', ()=>toggleDrawer(false));

    // ---------------------------
    // Android Dosya Seçimi
    // ---------------------------
    importFileItem.addEventListener('click', () => {
        if (window.Android && window.Android.pickFile) {
            window.Android.pickFile();
        } else {
            alert("Dosya seçici Android uygulamasında çalışır.");
        }
    });

    window.handleImportedFileFromAndroid = function(uri) {
        console.log("Seçilen dosya URI:", uri);
        // WebView tarafında işlemek için örnek
        const fileName = uri.split('/').pop();
        const newFile = {
            name: fileName,
            type: fileName.split('.').pop().toLowerCase(),
            date: new Date().toLocaleDateString(),
            size: '0 kB',
            isFavorite: false
        };
        allFiles.push(newFile);
        saveFiles();
        renderCurrentContent();
    }

    // ---------------------------
    // Render
    // ---------------------------
    function renderContent(files, targetElement, fileTypeFilter='all', sortCriteria='date-desc', searchTerm='') {
        targetElement.innerHTML = '';
        fileMenuPopup.style.display = 'none';

        let filesToRender = [...files];

        const typesMap = {
            'pdf': ['pdf'],
            'word': ['doc','docx'],
            'excel': ['xls','xlsx'],
            'ppt': ['ppt','pptx']
        };
        if(fileTypeFilter!=='all') filesToRender = filesToRender.filter(f => typesMap[fileTypeFilter].includes(f.type));
        if(searchTerm) filesToRender = filesToRender.filter(f => f.name.toLowerCase().includes(searchTerm.toLowerCase()));

        const activeNavId = document.querySelector('.bottom-nav .nav-item.active').dataset.nav;
        if(activeNavId==='favorites') filesToRender = filesToRender.filter(f => f.isFavorite);

        // Sorting
        filesToRender.sort((a,b)=>{
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

        if(filesToRender.length===0){
            targetElement.innerHTML = `<div class="empty-state">
                <i class="fas fa-folder-open fa-3x"></i>
                <p>${searchTerm?'Aradığınız dosya bulunamadı.':'Henüz hiç dosyanız yok'}</p>
            </div>`;
            return;
        }

        filesToRender.forEach(item=>{
            const fileItem = document.createElement('div');
            fileItem.className='file-item';
            if(selectionMode) fileItem.classList.add('selection-mode');

            let iconClass='fa-file';
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

    function renderCurrentContent(){
        const activeNav = document.querySelector('.bottom-nav .nav-item.active').dataset.nav;
        let targetElement = activeNav==='favorites'?favoritesListContent:fileListContent;
        const activeTab = document.querySelector('.top-tabs .tab.active');
        const activeTabType = activeTab ? activeTab.dataset.tab:'all';
        const currentSort = document.querySelector('.sort-option.active').dataset.sort;
        const currentSearchTerm = searchInput.value;
        renderContent(allFiles, targetElement, activeTabType, currentSort, currentSearchTerm);
    }

    // ---------------------------
// Selection İşlemleri
// ---------------------------
function updateSelectionCount(){
    selectionCount.textContent = `${selectedFiles.length} Dosya Seçildi`;
    selectionDelete.style.display = selectedFiles.length > 0 ? 'block' : 'none';
    selectionMove.style.display = selectedFiles.length > 0 ? 'block' : 'none';
    selectionShare.style.display = selectedFiles.length > 0 ? 'block' : 'none';
}

selectionDelete.addEventListener('click', deleteSelectedFiles);
selectionMove.addEventListener('click', () => { 
    alert("Dosyalar taşınıyor..."); 
    exitSelectionMode(); 
});
selectionShare.addEventListener('click', () => { 
    alert("Dosyalar paylaşılıyor..."); 
    exitSelectionMode(); 
});

function deleteSelectedFiles(){
    if(selectedFiles.length > 0 && confirm(`${selectedFiles.length} dosyayı silmek istediğinizden emin misiniz?`)){
        const selectedNames = selectedFiles.map(f => f.name);
        allFiles = allFiles.filter(f => !selectedNames.includes(f.name));
        saveFiles();
        exitSelectionMode();
    }
}

function enterSelectionMode(){
    selectionMode = true;
    selectedFiles = [];
    mainHeader.style.display = 'none';
    selectionHeader.style.display = 'flex';
    bottomNav.style.display = 'none';
    document.body.classList.add('selection-mode');
    renderCurrentContent();
    updateSelectionCount();
}

function exitSelectionMode(){
    selectionMode = false;
    selectedFiles = [];
    mainHeader.style.display = 'flex';
    selectionHeader.style.display = 'none';
    bottomNav.style.display = 'flex';
    document.body.classList.remove('selection-mode');
    renderCurrentContent();
}

// Select icon ve cancel
selectIcon.addEventListener('click', enterSelectionMode);
selectionCancel.addEventListener('click', exitSelectionMode);

// Select All
selectionAll.addEventListener('click', ()=>{
    const allCheckboxes = document.querySelectorAll('.file-checkbox');
    const allSelected = selectedFiles.length === allCheckboxes.length;
    selectedFiles = [];
    allCheckboxes.forEach(checkbox => {
        checkbox.checked = !allSelected;
        if(!allSelected){
            const fileName = checkbox.dataset.filename;
            const file = allFiles.find(f => f.name === fileName);
            if(file) selectedFiles.push(file);
        }
    });
    updateSelectionCount();
});
