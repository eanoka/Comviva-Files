import {Component, ElementRef, ViewChild} from "@angular/core";
import {ModalDirective} from "ngx-bootstrap/modal";
import * as $ from "jquery"

export let Alert: ConfirmDialogComponent;

@Component({
    selector: "global-alert-modal",
    templateUrl: './modal-confirm.component.html'
})
export class ConfirmDialogComponent {
    @ViewChild("modal") modal: ModalDirective

    public title: string
    public body: string
    public buttons = {
        cancel: true,
        abort: true
    }

    public handlers = {
        ok: null,
        cancel: null,
        abort: null
    }

    public labels = {
        ok: undefined,
        cancel: undefined
    }

    constructor(ref: ElementRef) {
        Alert = this
    }

    close(which: string) {
        if(this.handlers[which]) {
            this.handlers[which]()
        }
        this.modal.hide();
    }

    display(title: string, body: string, config: any) {
        this.buttons = $.extend({
            cancel: true,
            abort: true
        }, config.buttons)
        this.labels = $.extend({
            ok: "OK",
            cancel: "Cancel"
        }, config.labels)
        this.handlers = $.extend({
            ok: null,
            cancel: null,
            abort: null
        }, config.handlers)
        this.title = title
        this.body = body
        this.modal.show()
    }
}