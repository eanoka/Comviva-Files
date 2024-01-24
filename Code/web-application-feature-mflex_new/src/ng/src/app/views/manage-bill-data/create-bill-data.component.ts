import {ChangeDetectorRef, Component} from '@angular/core';
import {FilterPageData} from "./filter-page-data.model";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import {ActivatedRoute, Router} from "@angular/router";
import $ from "jquery";
import {Snackbar} from "../blocks/snackbar.component";
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {App as app} from "../../app.model";
import {BillData} from "./bill-data.model";

@Component({
  templateUrl: 'create-bill-data.component.html',
    selector: 'bordered-content-wrapper',
    styleUrls: ['create-bill-data.component.scss']
})
export class CreateBillDataComponent extends FormHandlerUtil<FilterPageData> {
    public data: FilterPageData = new FilterPageData();
    public uploading: boolean = false

    public formDataLoaded: boolean = false
    public billDataLoaded: boolean = false
    private editingBillData: BillData

    public boundModel: any = {
        id: null,
        subAccount: null,
        category: null,
        company: null,
        consumerId: null,
        mobileNo: null,
        additionalParams: null,
        alias: null
    }

    public categorySelectionUpdated: boolean = false;
    public companySelectionUpdated: boolean = false;

    public nullReference: any = null

    constructor(private ref: ChangeDetectorRef, private router: Router, private route: ActivatedRoute) {
        super('/billData/filterPageData', '/billData/create');
        this.submit.subscribe(x => {
            this.router.navigateByUrl("/manage-bill-data/edit-data")
        })
        let id = this.route.snapshot.queryParams.id
        if(id) {
            this.boundModel.id = id;
            this.loadBillDataDetail(id)
        } else {
            this.billDataLoaded = true
        }
    }

    private fillUpBoundModel() {
        this.boundModel.additionalParams = {}

        if(!this.editingBillData) {
            return
        }
        this.boundModel.id = this.editingBillData.id
        this.boundModel.subAccount = this.data.divisions.find(x => x.id == this.editingBillData.clientDivision.id)
        this.boundModel.mobileNo = this.editingBillData.msisdn
        this.boundModel.category = this.data.categories.find(x => x.id == this.editingBillData.company.category.id)
        this.boundModel.company = this.boundModel.category ? this.boundModel.category.companies.find(x => x.id == this.editingBillData.company.id) : null
        this.boundModel.consumerId = this.editingBillData.accountNo
        this.boundModel.additionalParams = this.editingBillData.billdataAddtionalField;
		this.editingBillData.billdataAddtionalField.forEach(x => {this.boundModel.additionalParams["ADDITIONAL." + x.fields.paramCode] = x.value})
        this.boundModel.alias = this.editingBillData.alias
    }

    private loadBillDataDetail(id: number) {
        this.http.get<BillData>(app.basePath + "/billData/getDetail?id=" + this.boundModel.id).subscribe(x => {
            this.editingBillData = x
            this.billDataLoaded = true
            if(this.formDataLoaded) {
                this.dataLoaded = true
                this.fillUpBoundModel()
            }
        })
    }

    protected onLoadData(x: FilterPageData) {
        this.data = x;
        if(!this.billDataLoaded) {
            this.dataLoaded = false
        } else {
            this.dataLoaded = true
            this.fillUpBoundModel()
        }
        this.formDataLoaded = true
        this.ref.detectChanges()
    }

	private filterAdditionalFields(obj: any): Map<string, object> {
        let map = new Map();
        Object.keys(obj).filter(o => o.startsWith("ADDITIONAL.")).forEach(key => {
            var key1 = key.split(".")[1];
            map[key1] = obj[key];
        });
        return map;
	}

    protected prepareSubmissionData(v: any): any {
		let map = this.filterAdditionalFields(v);
        return $.extend({}, v, {company: v.company.id, category: v.category.id, subAccount: v.subAccount.id,  additionalParams: map}
        )
    }

    public fileDropped(files: FileList): void {
        if(files.length > 1) {
            Snackbar.show("danger", "You have to choose only one file")
            return;
        }
        let file = files.item(0);
        if(!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
            Snackbar.show("danger", "Only excel files are supported")
            return
        }
        const formData: FormData = new FormData();
        formData.append('bulkBillData', file, file.name);
        this.uploading = true
        this.http.post<ApiPostResponse>(app.basePath + "/billData/uploadBulkFile", formData).subscribe(x => {
            Snackbar.show(x.code == 200 ? "success" : "danger", x.message);
            if(x.code == 200) {
                if (!x.message.endsWith("Failed: 0")) {
                    let instance = Snackbar.show("danger", "", false)
                    let interval
                    interval = setInterval(() => {
                        let dom = $("#snackbar-alert-" + instance + " .close")
                        if (dom.length) {
                            clearInterval(interval)
                            dom.after("<a download='Errors.xlsx' href='" + app.basePath + "/billData/errorFileDownload'>Download Error Report</a>")
                        }
                    }, 10)
                }
            }
            this.submit.emit(x)
            this.uploading = false
        }, err => {
            this.uploading = false
        });
    }

    protected readonly JSON = JSON;
}
