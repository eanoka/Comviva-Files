import {Component} from '@angular/core';
import {FilterPageData} from "./filter-page-data.model";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import $ from "jquery"
import {Snackbar} from "../blocks/snackbar.component";

@Component({
    templateUrl: 'refresh-bills.component.html',
    selector: 'bordered-content-wrapper'
})
export class RefreshBillsComponent extends FormHandlerUtil<FilterPageData> {
    public data: FilterPageData = new FilterPageData();
    public boundModel: any = {
        subAccount: [],
        category: null,
        company: null,
        consumerId: null
    }

    public categorySelectionUpdated: boolean = false;
    public companySelectionUpdated: boolean = false;

    public nullReference: any = null
    
    public divisionEntries: any[] = []

    constructor() {
        super('/billData/filterPageData', '/billData/queueRefreshRequest');
        this.submit.subscribe(x => {
            if(x.code == 200) {
                Snackbar.show("success", "You will be notified after collection completes");
            }
        })
    }

    protected prepareSubmissionData(v: any): any {
        return $.extend(
            {}, v, {company: v.company && v.company.id, category: v.category && v.category.id}
        )
    }

    protected onLoadData(x: FilterPageData) {
        this.divisionEntries = x.divisions
        this.data = x;
    }
}