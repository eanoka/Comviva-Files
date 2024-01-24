import {AfterViewInit, Component, ElementRef, HostBinding, ViewChild} from '@angular/core';
import {BillCollectionRequestService} from "./bill-collection-request.service";
import $ from 'jquery';
import {Util} from "../../util";
import {App} from "../../app.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import { BillDetailTask } from './bill-collection-request.model';

@Component({
	selector: 'bordered-content-wrapper',
	templateUrl: 'bill-collection-request.component.html'
})
export class BillCollectionRequestComponent implements AfterViewInit {

	public totalData: number = 0
	public offset: number = 0
	public records: BillDetailTask[]
	public _u: any = Util

	public permissions: string[] = App.user.permissions

	@HostBinding("class.busy")
	public tableDataLoading: boolean = false

	@ViewChild("tbody", { read: ElementRef }) tbody: ElementRef;
	@ViewChild(PaginatorComponent) page: PaginatorComponent;

	constructor(public dataService: BillCollectionRequestService) {
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
}