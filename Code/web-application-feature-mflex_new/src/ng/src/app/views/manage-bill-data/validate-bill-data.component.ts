import {AfterViewInit, Component, ElementRef, HostBinding, ViewChild} from '@angular/core';
import {ValidateDataService} from "./validate-data.service";
import {BillData} from "./bill-data.model";
import $ from 'jquery';
import {App} from "../../app.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";

@Component({
    templateUrl: 'validate-bill-data.component.html',
    selector: 'bordered-content-wrapper',
    styleUrls: ['validate-bill-data.component.scss']
})
export class ValidateDataComponent implements AfterViewInit {
    public totalData: number = 0
    public offset: number = 0
    public records: BillData[]
    public selectedIds: number[] = Array<number>()
    public comment: string
    public permissions: string[] = App.user.permissions

    public oldBilldata: BillData = new BillData()

    @HostBinding("class.busy")
    public tableDataLoading: boolean = false

    @ViewChild(PaginatorComponent) page: PaginatorComponent;

    constructor(public dataService: ValidateDataService) {
        dataService.load.subscribe(data => {
            this.totalData = data.count
            this.offset = data.offset
            this.records = data.records
            this.tableDataLoading = false
        })
    }

    ngAfterViewInit(): void {
        this.dataService.page = this.page
        this.dataService.beforeDataLoad = () => {
            this.tableDataLoading = true
        }
        this.dataService.loadTableData()
    }

    public selectAll(ev) {
        var element;
        if (!ev.target.checked) {
            this.selectedIds.forEach(selectedId => {
                element = <HTMLInputElement>document.getElementById("select-checkbox-" + selectedId);
                element.checked = false;
            })
            this.selectedIds = []
        } else {
            this.records.forEach(r => {
                this.selectedIds.push(r.id)
                element = <HTMLInputElement>document.getElementById("select-checkbox-" + r.id);
                element.checked = true;
            })
        }
    }

    public selectionChange(ev) {
        let _this = $(ev.target)
        let id: number = _this.attr("billdata-id")
        if (!ev.target.checked) {
            let unchecked: number = id;
            this.selectedIds = this.selectedIds.filter(obj => obj !== unchecked);
        } else {
            this.selectedIds.push(id)
        }
    }

    public approve() {
        if (this.selectedIds.length !== 0) {
            this.dataService.approveBills(this.selectedIds, this.comment, () => {
                this.dataService.loadTableData()
            })
        }
    }

    public reject() {
        if (this.selectedIds.length !== 0) {
            this.dataService.rejectBills(this.selectedIds, this.comment, () => {
                this.dataService.loadTableData()
            })
        }
    }
}