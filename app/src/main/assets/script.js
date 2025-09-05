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

    const importFileItem = document.getElementById('import-file-item');
    const importFileInput = document.getElementById('import-file-input');

    let allFiles = [];
    let selectionMode = false;
    let selectedFiles = [];

    function loadFiles() {
        const storedFiles = localStorage.getItem('allFiles');
        if (storedFiles) {
            allFiles = JSON.parse(storedFiles);
        }
    }

    function saveFiles() {
        localStorage.setItem('allFiles', JSON.stringify(allFiles));
    }

    const isDarkMode = localStorage.getItem('darkMode') === 'true';
    if (isDarkMode) {
        document.body.classList.add('dark-mode');
        darkModeToggle.checked = true;
    }

    function showContent(navId) {
        contentSections.forEach(section => {
            if (section.id === `${navId}-content`) {
                section.classList.add('active');
            } else {
                section.classList.remove('active');
            }
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

    function renderContent(files, targetElement, fileTypeFilter = 'all', sortCriteria = 'date-desc', searchTerm = '') {
        targetElement.innerHTML = '';
        fileMenuPopup.style.display = 'none';
    
        let filesToRender = [...files];
    
        if (fileTypeFilter !== 'all') {
            const types = {
                'pdf': ['pdf'],
                'word': ['docx', 'doc'],
                'excel': ['xlsx', 'xls'],
                'ppt': ['pptx', 'ppt']
            };
            filesToRender = filesToRender.filter(item => types[fileTypeFilter].includes(item.type));
        }
    
        if (searchTerm) {
            filesToRender = filesToRender.filter(item =>
                item.name.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
    
        const activeNavId = document.querySelector('.bottom-nav .nav-item.active').getAttribute('data-nav');
        if (activeNavId === 'favorites') {
            filesToRender = filesToRender.filter(item => item.isFavorite);
        }
    
        filesToRender.sort((a, b) => {
            if (sortCriteria === 'date-desc') {
                return new Date(b.date) - new Date(a.date);
            } else if (sortCriteria === 'date-asc') {
                return new Date(a.date) - new Date(b.date);
            } else if (sortCriteria === 'name-asc') {
                return a.name.localeCompare(b.name);
            } else if (sortCriteria === 'name-desc') {
                return b.name.localeCompare(a.name);
            } else if (sortCriteria === 'size-asc') {
                const sizeA = parseFloat(a.size.replace(',', '.').replace(' kB', ''));
                const sizeB = parseFloat(b.size.replace(',', '.').replace(' kB', ''));
                return sizeA - sizeB;
            } else if (sortCriteria === 'size-desc') {
                const sizeA = parseFloat(a.size.replace(',', '.').replace(' kB', ''));
                const sizeB = parseFloat(b.size.replace(',', '.').replace(' kB', ''));
                return sizeB - sizeA;
            }
            return 0;
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
            if (selectionMode) {
                fileItem.classList.add('selection-mode');
            }
    
            let iconClass = '';
            switch (item.type) {
                case 'pdf':
                    iconClass = 'fa-file-pdf';
                    break;
                case 'docx':
                case 'doc':
                    iconClass = 'fa-file-word';
                    break;
                case 'xlsx':
                case 'xls':
                    iconClass = 'fa-file-excel';
                    break;
                case 'ppt':
                case 'pptx':
                    iconClass = 'fa-file-powerpoint';
                    break;
                default:
                    iconClass = 'fa-file';
            }
            
            const isSelected = selectedFiles.some(f => f.name === item.name);

            fileItem.innerHTML = `
                <div class="file-item-wrapper">
                    <input type="checkbox" class="file-checkbox" ${isSelected ? 'checked' : ''} data-filename="${item.name}">
                    <div class="file-icon-container">
                        <i class="fas ${iconClass} file-icon"></i>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${item.name}</div>
                        <div class="file-details">${item.date} · ${item.size}</div>
                    </div>
                    <div class="file-actions">
                        <i class="favorite-icon ${item.isFavorite ? 'fas fa-star' : 'far fa-star'}" data-filename="${item.name}"></i>
                        <i class="fas fa-ellipsis-v file-menu" data-filename="${item.name}"></i>
                    </div>
                </div>
            `;
            
            const checkbox = fileItem.querySelector('.file-checkbox');
            const fileItemWrapper = fileItem.querySelector('.file-item-wrapper');

            fileItemWrapper.addEventListener('click', (e) => {
                if (selectionMode) {
                    if (e.target.classList.contains('file-item-wrapper') || e.target.closest('.file-info')) {
                        e.stopPropagation(); 
                        checkbox.checked = !checkbox.checked;
                        checkbox.dispatchEvent(new Event('change'));
                    }
                }
            });
            
            checkbox.addEventListener('change', (e) => {
                const fileName = e.target.getAttribute('data-filename');
                const file = allFiles.find(f => f.name === fileName);
                if (e.target.checked) {
                    selectedFiles.push(file);
                } else {
                    selectedFiles = selectedFiles.filter(f => f.name !== fileName);
                }
                updateSelectionCount();
            });
            

            const favoriteIcon = fileItem.querySelector('.favorite-icon');
            favoriteIcon.addEventListener('click', (e) => {
                e.stopPropagation();
                toggleFavorite(e.target.getAttribute('data-filename'));
            });

            const fileMenuIcon = fileItem.querySelector('.file-menu');
            fileMenuIcon.addEventListener('click', (e) => {
                e.stopPropagation();
                showFileMenu(e.target, e.target.getAttribute('data-filename'));
            });
    
            targetElement.appendChild(fileItem);
        });
    }

    function updateSelectionCount() {
        selectionCount.textContent = `${selectedFiles.length} Dosya Seçildi`;
        selectionDelete.style.display = selectedFiles.length > 0 ? 'block' : 'none';
        selectionMove.style.display = selectedFiles.length > 0 ? 'block' : 'none';
        selectionShare.style.display = selectedFiles.length > 0 ? 'block' : 'none';
    }

    function toggleFavorite(fileName) {
        const file = allFiles.find(f => f.name === fileName);
        if (file) {
            file.isFavorite = !file.isFavorite;
            saveFiles();
            renderCurrentContent();
        }
    }
    
    function renameFile(oldName) {
        const file = allFiles.find(f => f.name === oldName);
        if (file) {
            const newName = window.prompt("Dosyayı yeniden adlandır:", file.name);
            if (newName && newName !== file.name) {
                file.name = newName;
                saveFiles();
                renderCurrentContent();
            }
        }
    }
    
    function deleteFile(fileName) {
        if (window.confirm(`${fileName} dosyasını silmek istediğinizden emin misiniz?`)) {
            allFiles = allFiles.filter(f => f.name !== fileName);
            saveFiles();
            renderCurrentContent();
        }
    }

    function deleteSelectedFiles() {
        if (selectedFiles.length > 0 && window.confirm(`${selectedFiles.length} dosyayı silmek istediğinizden emin misiniz?`)) {
            const selectedNames = selectedFiles.map(f => f.name);
            allFiles = allFiles.filter(f => !selectedNames.includes(f.name));
            saveFiles();
            exitSelectionMode();
        }
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
    
    function showFileMenu(targetIcon, fileName) {
        const rect = targetIcon.getBoundingClientRect();
        fileMenuPopup.style.top = `${rect.bottom + 5}px`;
        fileMenuPopup.style.left = `${rect.left - 150}px`;
        fileMenuPopup.style.display = 'block';
        fileMenuPopup.setAttribute('data-target-file', fileName);

        const file = allFiles.find(f => f.name === fileName);
        const favoriteMenuItem = fileMenuPopup.querySelector('.menu-favorite span');
        favoriteMenuItem.textContent = file.isFavorite ? 'Favorilerden Kaldır' : 'Favorilere Ekle';
        fileMenuPopup.querySelector('.menu-favorite i').className = file.isFavorite ? 'fas fa-star' : 'far fa-star';
    }

    function renderCurrentContent() {
        const activeNav = document.querySelector('.bottom-nav .nav-item.active').getAttribute('data-nav');
        let targetElement = fileListContent;
        let filesToRender = allFiles;

        if (activeNav === 'favorites') {
            targetElement = favoritesListContent;
        } else if (activeNav === 'all-files') {
             targetElement = fileListContent;
        }

        const activeTab = document.querySelector('.top-tabs .tab.active');
        const activeTabType = activeTab ? activeTab.getAttribute('data-tab') : 'all';
        const currentSort = document.querySelector('.sort-option.active').getAttribute('data-sort');
        const currentSearchTerm = searchInput.value;

        renderContent(filesToRender, targetElement, activeTabType, currentSort, currentSearchTerm);
    }
    
    function enterSelectionMode() {
        selectionMode = true;
        selectedFiles = [];
        mainHeader.style.display = 'none';
        selectionHeader.style.display = 'flex';
        bottomNav.style.display = 'none';
        document.body.classList.add('selection-mode');
        renderCurrentContent();
        updateSelectionCount();
    }
    
    function exitSelectionMode() {
        selectionMode = false;
        selectedFiles = [];
        mainHeader.style.display = 'flex';
        selectionHeader.style.display = 'none';
        bottomNav.style.display = 'flex';
        document.body.classList.remove('selection-mode');
        renderCurrentContent();
    }
    
    selectIcon.addEventListener('click', enterSelectionMode);
    selectionCancel.addEventListener('click', exitSelectionMode);
    
    selectionAll.addEventListener('click', () => {
        const allCheckboxes = document.querySelectorAll('.file-checkbox');
        const allSelected = allCheckboxes.length > 0 && selectedFiles.length === allCheckboxes.length;
        
        selectedFiles = [];
        allCheckboxes.forEach(checkbox => {
            if (!allSelected) {
                checkbox.checked = true;
                const fileName = checkbox.getAttribute('data-filename');
                const file = allFiles.find(f => f.name === fileName);
                if (file) {
                    selectedFiles.push(file);
                }
            } else {
                checkbox.checked = false;
            }
        });
        updateSelectionCount();
    });
    
    document.addEventListener('click', (e) => {
        if (!fileMenuPopup.contains(e.target) && !e.target.classList.contains('file-menu')) {
            fileMenuPopup.style.display = 'none';
        }
    });

    fileMenuPopup.addEventListener('click', (e) => {
        const action = e.target.closest('li').getAttribute('data-action');
        const fileName = fileMenuPopup.getAttribute('data-target-file');

        if (action === 'toggle-favorite') {
            toggleFavorite(fileName);
        } else if (action === 'rename') {
            renameFile(fileName);
        } else if (action === 'delete') {
            deleteFile(fileName);
        }
        fileMenuPopup.style.display = 'none';
    });

    searchIcon.addEventListener('click', () => {
        headerTitle.style.display = 'none';
        headerIcons.style.display = 'none';
        drawerToggle.style.display = 'none';
        backButton.style.display = 'none';
        searchBar.style.display = 'flex';
        searchInput.focus();
    });

    searchBack.addEventListener('click', () => {
        headerTitle.style.display = 'block';
        headerIcons.style.display = 'flex';
        drawerToggle.style.display = 'block';
        backButton.style.display = 'none';
        searchBar.style.display = 'none';
        searchInput.value = '';
        renderCurrentContent();
    });

    searchClear.addEventListener('click', () => {
        searchInput.value = '';
        searchInput.focus();
        renderCurrentContent();
    });

    searchInput.addEventListener('input', () => {
        renderCurrentContent();
    });

    sortIcon.addEventListener('click', () => {
        sortPopup.style.display = 'flex';
    });

    sortCancelButton.addEventListener('click', () => {
        sortPopup.style.display = 'none';
    });

    sortOptions.forEach(option => {
        option.addEventListener('click', () => {
            sortOptions.forEach(opt => opt.classList.remove('active'));
            option.classList.add('active');
        });
    });

    sortApplyButton.addEventListener('click', () => {
        sortPopup.style.display = 'none';
        renderCurrentContent();
    });

    topTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            topTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            renderCurrentContent();
        });
    });

    function toggleDrawer(open) {
        if (open) {
            drawer.classList.add('active');
            overlay.classList.add('active');
        } else {
            drawer.classList.remove('active');
            overlay.classList.remove('active');
        }
    }

    drawerToggle.addEventListener('click', () => {
        toggleDrawer(true);
    });

    overlay.addEventListener('click', () => {
        toggleDrawer(false);
    });

    backButton.addEventListener('click', () => {
        bottomNavItems.forEach(item => item.classList.remove('active'));
        document.querySelector('.nav-item[data-nav="all-files"]').classList.add('active');
        document.querySelector('.top-tabs .tab[data-tab="all"]').classList.add('active');
        showContent('all-files');
    });

    bottomNavItems.forEach(item => {
        item.addEventListener('click', () => {
            const navId = item.getAttribute('data-nav');
            bottomNavItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
            showContent(navId);
        });
    });

    let touchStartX = 0;
    document.addEventListener('touchstart', (e) => {
        touchStartX = e.touches[0].clientX;
    });

    document.addEventListener('touchend', (e) => {
        const touchEndX = e.changedTouches[0].clientX;
        const deltaX = touchEndX - touchStartX;

        if (deltaX < -50 && drawer.classList.contains('active')) {
            toggleDrawer(false);
        } else if (deltaX > 50 && !drawer.classList.contains('active')) {
            toggleDrawer(true);
        }
    });

    darkModeToggle.addEventListener('change', (e) => {
        if (e.target.checked) {
            document.body.classList.add('dark-mode');
            localStorage.setItem('darkMode', 'true');
        } else {
            document.body.classList.remove('dark-mode');
            localStorage.setItem('darkMode', 'false');
        }
    });

    importFileItem.addEventListener('click', () => {
        importFileInput.click();
    });

    importFileInput.addEventListener('change', (event) => {
        const files = event.target.files;
        if (files.length > 0) {
            Array.from(files).forEach(file => {
                const fileType = file.name.split('.').pop().toLowerCase();
                const fileSize = (file.size / 1024).toFixed(2);

                allFiles.push({
                    name: file.name,
                    type: fileType,
                    date: new Date().toLocaleDateString('tr-TR'),
                    size: `${fileSize} kB`,
                    isFavorite: false
                });
            });
            saveFiles();
            renderCurrentContent();
            toggleDrawer(false);
        }
    });

    loadFiles();
    document.querySelector('.nav-item[data-nav="all-files"]').classList.add('active');
    document.querySelector('.top-tabs .tab[data-tab="all"]').classList.add('active');
    document.querySelector('.sort-option[data-sort="date-desc"]').classList.add('active');
    showContent('all-files');
});
