// ==UserScript==
// @name        New script
// @namespace   Violentmonkey Scripts
// @match       https://hosted.weblate.org/projects/ffupdater/*
// @grant       GM_xmlhttpRequest
// @version     1.0
// @author      -
// @description 02/02/2023, 10:54:50
// ==/UserScript==

const button = document.createElement("button");
button.innerText = "Generate text for changelog";


function getDateOfLastFFUpdaterRelease() {
    return new Promise(resolve => {
        let control = GM_xmlhttpRequest({
            url: 'https://api.github.com/repos/Tobi823/ffupdater/releases/latest',
            method: 'GET',
            responseType: 'json',
            onload: function (rq) {
                console.log(`Request result: ${rq}`);

                let latestRelease = JSON.parse(rq.responseText);
                console.log(`JSON: ${latestRelease}`);

                let date = latestRelease['created_at'].split('T')[0];
                console.log(`Latest FFUpdater release date: ${latestRelease}`);
                resolve(date);
            },
            onerror: function (e) {
                console.error(e);
            }
        })
    });
}

function getContributorStats(startDate) {
    return new Promise(resolve => {
        let formData = new FormData();
        let csrf = document.getElementsByName('csrfmiddlewaretoken')[0].value;
        console.log(`CSRF: ${csrf}`);
        formData.append('csrfmiddlewaretoken', csrf);
        formData.append('style', 'json');
        formData.append('period', '');
        formData.append('start_date', startDate);
        formData.append('end_date', new Date().toISOString().split('T')[0]);

        let control = GM_xmlhttpRequest({
            url: 'https://hosted.weblate.org/counts/ffupdater/',
            method: 'POST',
            headers: {
                'Referer': 'https://hosted.weblate.org/'
            },
            data: formData,
            responseType: 'json',
            onload: function (rq) {
                console.log(`Request result: ${rq}`);

                let users = JSON.parse(rq.responseText);
                console.log(`JSON: ${users}`);

                let userNames = users.sort((a, b) => b['t_words'] - a['t_words'])
                    .filter(user => user['name'] !== 'Tobias Hellmann')
                    .map(user => user['name']);
                console.log(`Users: ${userNames}`);

                let lastUserName = userNames.pop();
                let userNamesAsOneString = `${userNames.join(', ')} and ${lastUserName}`;
                let output = `Thanks for the Weblate contributions from: ${userNamesAsOneString}`;
                console.log(output);
                resolve(output);
            },
            onerror: function (e) {
                console.error(e);
            }
        });
    });
}

button.addEventListener("click", async () => {
    let startDate = await getDateOfLastFFUpdaterRelease();
    let contributors = await getContributorStats(startDate);

    const span = document.createElement("span");
    span.innerText = contributors;
    document.getElementById("reports").append(span);
});

document.getElementById("reports").append(button)



