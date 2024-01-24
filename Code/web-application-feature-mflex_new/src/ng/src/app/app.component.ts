import {Component, OnInit} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {App} from './app.model'
import {HttpClient} from '@angular/common/http';
import * as SockJS from 'sockjs-client'
import {Stomp} from 'stompjs/lib/stomp.js'
import {Alert} from "./views/blocks/modal-confirm.component";
import {SessionUser} from "./views/user-management/session-user.model";
import {Snackbar} from "./views/blocks/snackbar.component";

@Component({
    selector: 'body',
    template: '<router-outlet></router-outlet>'
})
export class AppComponent implements OnInit {
    constructor(private router: Router, private http: HttpClient) {
    }

    ngOnInit() {
        //Collecting session user object detail
        this.http.get(App.basePath + "/auth/userDetails").subscribe((data: SessionUser) => {
            App.user = data
            App.event.trigger("_USER_LOADED_", App.user);
        })
        this.http.get(App.basePath + "/app/getFrontendProperties").subscribe((data: any) => {
            App.setConfig(data)
        })

        this.router.events.subscribe((evt) => {
            if (!(evt instanceof NavigationEnd)) {
                return;
            }
            window.scrollTo(0, 0);
        });

        App.event.on("_USER_LOADED_", () => {
            Stomp.over(new SockJS(App.basePath + '/cbp-notification-socket')).connect({}, function () {
                this.subscribe(`/socket/notifier/global/notify`, function (frame) {
                    const body = JSON.parse(frame.body)
                    console.log('general sock message received')
                    console.dir(body)
                    App.event.trigger('__SOCK_MSG_' + body.topic, body)
                });
                this.subscribe(`/socket/notifier/user-${App.user.id}/notify`, function (frame) {
                    const body = JSON.parse(frame.body)
                    console.log('user sock message received')
                    console.dir(body)
                    App.event.trigger('__SOCK_MSG_' + body.topic, body)
                });
                this.subscribe(`/socket/notifier/${App.user.session}/notify`, function (frame) {
                    const body = JSON.parse(frame.body)
                    console.log('session sock message received')
                    console.dir(body)
                    App.event.trigger('__SOCK_MSG_' + body.topic, body)
                    let handler = () => {
                        location.href = App.basePath + "/saml/logout";
                    }
                    if(body.message == "logout") {
                        Alert.display("Logout!!", "You have been logged out. Click OK to relogin", {
                            buttons: {cancel: false}, handlers: {ok: handler, abort: handler}
                        })
                    }
                    if(body.topic == "access_permissions" && body.message == "modified") {
                        Alert.display("Permission!!", "Someone has changed your permission. Please reload to make that effective.", {
                            labels: {ok: "Reload", cancel: "Ignore"}, handlers: {
                                ok: () => {
                                    location.reload()
                                }
                            }
                        })
                    }
                });
            });
        })

        App.event.on("__SOCK_MSG_BCR_COMPLETE", (ev, data) => {
            Snackbar.show("success", "<span class='message'>" + data.message + "</span><a href='#/payment-request/create' class='btn dismiss'>Initiate</a>", false);
        })

        App.event.on("__SOCK_MSG_PR_COMPLETE", (ev, data) => {
            Snackbar.show("success", data.message);
        })
    }
}