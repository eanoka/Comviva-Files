import {HttpClient} from "@angular/common/http";
import {App as app} from "../app.model";
import {AppInjector} from "../app.module";
import {Snackbar} from "../views/blocks/snackbar.component";
import {ApiPostResponse} from "./api-post-response.model";
import {Directive, EventEmitter, Output} from "@angular/core";
import {Alert} from "../views/blocks/modal-confirm.component";
import { Injectable } from "@angular/core";

type MockNgForm = {
    valid: boolean
    value: any
} & {
    [key: string]: any
}

@Injectable({
  providedIn: 'root'
})
export abstract class FormHandlerUtil<F> {
    public submitting: boolean = false
    public dataLoaded: boolean = false;

    public confirmationRequired: boolean = false
    public confirmationMessage: ((form: MockNgForm) => string) | string = null

    @Output() submit: EventEmitter<ApiPostResponse> = new EventEmitter();

    protected http: HttpClient = AppInjector.get(HttpClient);

    constructor(protected dataFetcherUrl: string, protected dataSubmissionUrl: string, protected submitAsJson: boolean = true) {
        this.loadData()
    }

    protected loadData(): void {
        if(!this.dataFetcherUrl) {
            this.dataLoaded = true;
            return
        }
        this.dataLoaded = false
        this.http.get<F>(app.basePath + this.dataFetcherUrl).subscribe(x => {
            this.dataLoaded = true;
            this.onLoadData(x)
        })
    }

    protected abstract onLoadData(x: F): void;

    protected prepareSubmissionData(v: any): any {
        if(!this.submitAsJson) {
            let x = new FormData()
            for(let a in v) {
                x.append(a, v[a])
            }
            return x
        }
        return v
    }

    onSubmit(form: MockNgForm) {
        if(!this.dataSubmissionUrl) {
            return
        }
        this.submitting = true
        let submitFinal = () => {
            this.http.post<ApiPostResponse>(app.basePath + this.dataSubmissionUrl, this.prepareSubmissionData(form.value)).subscribe(x => {
                if(x.hasOwnProperty("code")) {
                    Snackbar.show(x.code == 200 ? "success" : "danger", x.message);
                }
                this.submit.emit(x)
                this.submitting = false
            }, err => {
                this.submitting = false
            })
        }
        if(form.valid) {
            if(this.confirmationRequired) {
                let message = this.confirmationMessage
                if(message instanceof Function) {
                    message = message(form)
                }
                Alert.display("Are You Sure!!", message, {
                    handlers: {ok: submitFinal}
                })
            } else {
                submitFinal()
            }
        }
    }
}
