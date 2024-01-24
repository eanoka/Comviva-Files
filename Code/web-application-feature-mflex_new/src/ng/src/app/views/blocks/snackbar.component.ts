import {Component, HostListener} from "@angular/core";
import {AlertConfig} from "ngx-bootstrap/alert";
import * as $ from "jquery"

export let Snackbar: SnackbarComponent;

@Component({
    templateUrl: './snackbar.component.html',
    selector: 'app-snackbar',
    styleUrls: ['./snackbar.component.scss'],
    providers: [{ provide: AlertConfig}]
})
export class SnackbarComponent {
    alerts: any = [];
    timeout = 5000;
    maxDisplay = 10;
    instance = 0

    @HostListener('click', ['$event.target'])
    onClick(elem) {
        if($(elem).is(".dismiss")) {
            $(elem).closest(".alert").find(".close").trigger("click")
        }
    }

    constructor() {
        Snackbar = this
    }

    show(type: string, msg: string, autoDismiss: boolean = true): number {
        let html = undefined
        if(msg.startsWith("<")) {
            html = msg
            msg = null
        }
        if(this.alerts.length == this.maxDisplay) {
            this.alerts.shift()
        }
        this.alerts.push({
            type: type, msg: msg, html: html, timeout: autoDismiss ? this.timeout : 300000, dismissible: autoDismiss ? false : true, id: ++this.instance
        })
        return this.instance
    }
}