var entityMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': '&quot;',
    "'": '&#39;',
    "/": '&#x2F;'
};

function escapeHtml(string) {
    return String(string).replace(/[&<>"'\/]/g, function (s) {
        return entityMap[s];
    });
}

function popup(mylink, windowname) {
    return popupSize(mylink, windowname, 400, 200);
}

function popupSize(mylink, windowname, width, height) {
    var href;
    if (typeof(mylink) == "string") {
        href = mylink;
    } else {
        href = mylink.href;
    }

    var w = window.open(href, windowname, "width=" + width + ",height=" + height + ",scrollbars=yes,resizable=yes");
    w.focus();
    w.moveTo(300, 200);
    return false;
}

function updateQueryStringParameter(uri, key, value) {
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    if (uri.match(re)) {
        return uri.replace(re, '$1' + key + "=" + value + '$2');
    } else {
        var separator = uri.indexOf('?') !== -1 ? "&" : "?";
        return uri + separator + key + "=" + value;
    }
}

function formatDuration(seconds) {
    const value = Math.abs(seconds);
    const days = Math.floor(value / 86400);
    const hours = Math.floor((value - (days * 86400)) / 3600);
    const min = Math.floor((value - (days * 86400) - (hours * 3600)) / 60);
    const sec = value - (days * 86400) - (hours * 3600) - (min * 60);
    var res = '';
    if (days > 0) { res = days + 'd.'; }
    if (hours >= 10) {
        res = res + hours + ':';
    } else {
        if (hours == 0 && days == 0) {
            // do nothing
        } else {
            res = res + '0' + hours + ':';
        }
    }

    if (min >= 10) {
        res = res + min + ':';
    } else {
        res = res + '0' + min + ':';
    }

    if (sec >= 10) {
        res = res + sec;
    } else {
        res = res + '0' + sec;
    }

    if (seconds < 0) { res = '-' + res; }
    return res;
}

function getJQueryUiDialogPlaylistSize(origin) {
    var width = window.localStorage.getItem("dialog-select-playlist-" + origin + "-width");
    var height = window.localStorage.getItem("dialog-select-playlist-" + origin + "-height");
    if (!width) {
        width = 300;
    }
    if (!height) {
        height = 250;
    }
    return {width: width, height: height};
}

function setJQueryUiDialogPlaylistSize(origin, size) {
    window.localStorage.setItem("dialog-select-playlist-" + origin + "-width", parseInt(size.width));
    window.localStorage.setItem("dialog-select-playlist-" + origin + "-height", parseInt(size.height));
}
