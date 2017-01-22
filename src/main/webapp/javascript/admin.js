var USERID_MIN_LENGTH = 3;
var USERID_MAX_LENGTH = 32;
var PASSWORD_MIN_LENGTH = 5;
var PASSWORD_MAX_LENGTH = 32;

function confirmDelete(delUser, userHomeDir) {
    if (confirm("Are you sure you want to delete user " + delUser + " ?\nThis will delete all data in the home directory of this user:\n" + userHomeDir)) {
        window.location.href = getContextRoot() + "/servlet?command=admin&cmd=deleteUser&userToBeDeleted=" + encodeURIComponent(delUser);
    }
}

function diskQuota(userid) {
    window.open(getContextRoot() + "/servlet?command=diskQuota&userid=" + encodeURIComponent(userid), "quotaWin", "scrollbars=no,resizable=no,width=400,height=230,left=100,top=100,screenX=100,screenY=100");
}

function switchDiskQuota(checkbox) {
    if (checkbox.checked) {
        document.getElementById('diskQuota').disabled = false;
        document.getElementById('diskQuota').focus();
    } else {
        document.getElementById('diskQuota').value = "";
        document.getElementById('diskQuota').disabled = true;
    }
}

function userDiskQuota(userid) {
    var diskQuotaCont = document.getElementById("diskQuotaCont");
        
    if (!diskQuotaCont) {
        return;
    }
        
    var contextRoot = getContextRoot();    
        
    var xmlUrl = contextRoot + "/servlet?command=admin&cmd=userDiskQuota&userName=" + encodeURIComponent(userid);
        
    var xslUrl = contextRoot + "/xsl/userDiskQuota.xsl";    
        
    htmlFragmentByXslt(xmlUrl, xslUrl, diskQuotaCont, function() {
        setBundleResources();
        centerBox(diskQuotaCont);
        diskQuotaCont.style.visibility = "visible";
    });
}

function hideDiskQuota() {
    var diskQuotaCont = document.getElementById("diskQuotaCont");
        
    if (!diskQuotaCont) {
        return;
    }
    
    diskQuotaCont.style.visibility = "hidden";
}

function validateUser(isEdit) {

    clearValidationErrors();

    var validationError = false;
    
    if (!isEdit) {

        var userid = trim(document.getElementById('username').value);
    
        if (userid.length < USERID_MIN_LENGTH) {
            addValidationError("username", "the minimum length of the userid is " + USERID_MIN_LENGTH + " characters");
            validationError = true;
        }
    
        if (userid.length > USERID_MAX_LENGTH) {
            addValidationError("username", "the maximum length of the userid is " + USERID_MAX_LENGTH + " characters");
            validationError = true;
        }

        if (!checkUserid(userid)) {
            addValidationError("username", "The userid contains illegal characters. Allowed characters are: a-z, A-Z, 0-9, dash and dot. ");
            validationError = true;
        }
    }

    var password = trim(document.getElementById('password').value);
    var pwconfirm = trim(document.getElementById('pwconfirm').value);
    
    if (!isEdit) {
        if (password.length < PASSWORD_MIN_LENGTH) {
            addValidationError("password", "the minimum length of the password is " + PASSWORD_MIN_LENGTH + " characters");
            validationError = true;
        }
    }
    
    if (password.length > PASSWORD_MAX_LENGTH) {
        addValidationError("password", "the maximum length of the password is " + PASSWORD_MAX_LENGTH + " characters");
        validationError = true;
    }
    
    if (password.indexOf(' ') >= 0) {
        addValidationError("password", "the password must not contain spaces");
        validationError = true;
    }
    
    if (password != pwconfirm) {
        addValidationError("pwconfirm", "password and password confirmation must be equal");
        validationError = true;
    }

    var email = trim(document.getElementById('email').value);
    
    if (email.length == 0) {
        addValidationError("email", "e-mail address is a required field");
        validationError = true;
    } else if (!validateEmail(email)) {
        addValidationError("email", "the e-mail address does not conform to the required format");
        validationError = true;
    }

    var diskQuotaCheckbox = document.getElementById("checkDiskQuota");
    
    var diskQuotaValue = trim(document.getElementById('diskQuota').value);
    
    if (diskQuotaCheckbox.checked) {
        if (diskQuotaValue.length == 0) {
            addValidationError("diskQuota", "please enter a disk quota value");
            validationError = true;
        } else {
            if (isNaN(diskQuotaValue)) {
                addValidationError("diskQuota", "please enter a number for the disk quota value");
                validationError = true;
            } else {
                var diskQuotaNum = Number(diskQuotaValue);
                if ((diskQuotaNum < 1) || (diskQuotaNum > 10000000)) {
                    addValidationError("diskQuota", "the disk quota value must be in the range between 1 and 10000000 (MByte)");
                    validationError = true;
                }
            }
        }
    }

    if (!isEdit) {
        var languageSelect = document.getElementById('language');

        if (languageSelect.selectedIndex == 0) {
            addValidationError("language", "please select a language");
            validationError = true;
        }
    }
	
    if (!validationError) {
        document.getElementById('userForm').submit();
    }
    
    return (!validationError);
}

function clearValidationErrors() {
    removeAllChildNodes("validationErrorList");
    removeCSSRecursive(document.getElementById("userForm"), "validationError");
}

function addValidationError(inputId, errorMsg) {
    var validationErrorList = document.getElementById("validationErrorList");

	var listElem = document.createElement("li");
	listElem.setAttribute("class", "validationError");
    var msgTextNode = document.createTextNode(errorMsg);
	listElem.appendChild(msgTextNode);	
   	validationErrorList.appendChild(listElem);
   	
   	if (inputId != null) {
   	    var invalidInput = document.getElementById(inputId);
   	    invalidInput.setAttribute("class", "validationError");
    }	
}

function checkUserid(userid) {
    if (trim(userid).length == 0) {
	    return false;
	}
	    
    for (var i = 0; i < userid.length; i++) {
	    var c = userid.charAt(i);
	        
	    var charValid = false;
	    if (((c >= 'a') && (c <= 'z')) ||
	        ((c >= 'A') && (c <= 'Z')) ||
            ((c >= '0') && (c <= '9')) ||
	        (c == '-') ||
	        (c == '.') ||
            (c == 'ä') || (c == 'Ä') ||
            (c == 'ö') || (c == 'Ö') ||
            (c == 'ü') || (c == 'Ü') ||
            (c == 'ß')) {
	        charValid = true;
	    }
	    if (!charValid) {
	        return false;
	    }
	}
	    
    return true;
}
