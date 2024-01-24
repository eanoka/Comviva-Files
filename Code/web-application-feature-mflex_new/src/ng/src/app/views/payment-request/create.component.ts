import {Component, ElementRef, HostBinding, ViewChild} from '@angular/core';
import {FilterPageData} from "./filter-page-data.model";
import {FormHandlerUtil} from "../../generic/form-handler.util";
import $ from "jquery"
import {PaymentRequestService} from "./parment-request.service";
import {Bill, PaginatedBill} from "./bill.model";
import {App as app, App} from "../../app.model";
import {Snackbar} from "../blocks/snackbar.component";
import * as moment from "moment"
import {ApiPostResponse} from "../../generic/api-post-response.model";
import {ActivatedRoute, Router} from "@angular/router";
import {Actions} from "../../generic/actions.consts";
import {SessionUser} from "../user-management/session-user.model";
import {PaginatorComponent} from "../../generic/components/paginator.component";
import {ModalDirective} from "ngx-bootstrap/modal";
import {Util} from "../../util";
import {Company} from "../manage-bill-data/company.model";

@Component({
    templateUrl: 'create.component.html',
    selector: 'bordered-content-wrapper',
    styleUrls: ['create.component.css']
})
export class CreatePaymentRequestComponent extends FormHandlerUtil<FilterPageData> {
    public data: FilterPageData = new FilterPageData();
    public boundModel: any = {
        subAccount: [],
        category: null,
        company: null,
        consumerId: null
    }
    public selectedsShowing: boolean = false

    @HostBinding("class.busy")
    public tableDataLoading: boolean = false

    public bills: PaginatedBill = new PaginatedBill()
    public totalSelectedAmount: number = 0
    public selectedCount: number = 0
    public selectedBills: any = {}
    public formSubmittedData: any
    public lastFilterData: any

    public loggedUser: SessionUser = App.user
    public Util: any = Util

    public categorySelectionUpdated: boolean = false;
    public companySelectionUpdated: boolean = false;
    public nullReference: any = null
    public divisionEntries: any[] = []

    public _moment: any = moment
    public $: any = $
    public uploading: boolean = false
	public paymentrequestid: number = 0
	public attachedFile: File

	public headerAdditionalFieldsDataModel: Company[]
	public rowAdditionalFieldsDataModel: any = {}
	@ViewChild("headerAdditionalFieldModal", {read: ElementRef}) headerAdditionalFieldModal: ElementRef;
	@ViewChild("rowAdditionalFieldModal", {read: ElementRef}) rowAdditionalFieldModal: ElementRef;

    constructor(private paymentService: PaymentRequestService, private router: Router, private route: ActivatedRoute) {
        super('/billData/filterPageData', paymentService.dataLoadUrl = '/paymentRequest/filteredBillDataList');
        paymentService.beforeDataLoad = (pagePayLoad) => {
            this.tableDataLoading = true
            $.extend(pagePayLoad, this.formSubmittedData)
        }
        //After change in pagination variables
        paymentService.load.subscribe(data => {
            this.onLoadFilteredBills(data)
            this.tableDataLoading = false
        })
        this.submit.subscribe(y => {
            this.lastFilterData = this.formSubmittedData
            this.onLoadFilteredBills(y)
        })
 
        let oldRequestId = this.route.snapshot.queryParams.id;
		if (oldRequestId) {
			this.http.post<PaginatedBill>(app.basePath + '/paymentRequest/filteredBillDataList', {offset: 0, totalPerPage: 10}).subscribe(x => {
				x.records.forEach(y => {
					if (y.dueDate) {
						y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
					}
					if (y.syncDate) {
						y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
					}
				})
				this.bills = x
			})
			let page = new PaginatorComponent();
            page.offset = 0
            page.currentPerPage = this.bills.perPage
            this.paymentService.loadTableData(page)
			this.populateSelectedBillsForPaymentRequest(oldRequestId)
		}
    }
    
	private populateSelectedBillsForPaymentRequest(requestId: number) {
		this.http.post<Bill[]>(App.basePath + "/paymentRequest/getPaybleBillsForRequest", {requestId: requestId}).subscribe(x => {
			x.forEach(y => {
				this.selectBill(y, y.billAmount)
			})
		})
	}
 
 	private onLoadFilteredBills(bills: PaginatedBill) {
        bills.records.forEach(y => {
            if(y.dueDate) {
                y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
            }
            if(y.syncDate) {
                y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
            }
        })
        this.selectedsShowing = false
        this.bills = bills
    }

    /**
     * Submission of filter form data
     * @param v
     * @protected
     */
    protected prepareSubmissionData(v: any): any {
        return $.extend({}, this.formSubmittedData = $.extend(
            {}, v, {company: v.company && v.company.id, category: v.category && v.category.id}
        ), {offset: this.bills.offset, totalPerPage: this.bills.perPage})
    }

    protected onLoadData(x: FilterPageData) {
        this.divisionEntries = x.divisions
        this.data = x;
    }

    public selectionChange(ev, bill: Bill) {
        let _this = $(ev.target)
        let amountCell = _this.parent().siblings(".amount-cell")
        let amount = amountCell.has("input").length ? (+amountCell.find("input").val() || 1) : (bill.billAmount + bill.serviceCharge)
        if(ev.target.checked) {
            if(bill.company.hasBill && this.lastSyncWasBeforeNDays(5, bill)) {
                Snackbar.show("warning", "This bill was last synced more than 5 days ago")
            }
            this.selectBill(bill, amount)
        } else {
            this.unSelectBill(bill)
        }
        if(this.selectedsShowing) {
            this.showSelecteds(this.bills.offset, this.bills.perPage)
        }
    }

    private lastSyncWasBeforeNDays(n: number, bill: Bill) : boolean {
        return bill.syncDate && bill.syncDate < new Date(new Date().getTime() - n * 24 * 60 * 60 * 1000)
    }

    private selectBill(bill: Bill, amount: number) {
        let additionalFields
        try { additionalFields = bill.billRevertibleCache?.valuesAsJson ? JSON.parse(bill.billRevertibleCache?.valuesAsJson) : {} } catch {additionalFields = {}}
        this.selectedBills[bill.id] = {amount: amount, bill: bill, additionalFields: additionalFields}
        this.totalSelectedAmount += amount
        this.selectedCount += 1
    }

    private unSelectBill(bill: Bill) {
        this.totalSelectedAmount -= this.selectedBills[bill.id].amount
        delete this.selectedBills[bill.id]
        this.selectedCount -= 1
    }
    
    public hasAnyNonBillableUtilitySelected() : boolean {
        for(let billId in this.selectedBills) {
            let selectedBill = this.selectedBills[billId]
            if(!selectedBill.bill.company.hasBill) {
                return true
            }
        }
            return false
        }

    public isAnyUtilitySelectedWithAdditionalField() : boolean {
        for(let billId in this.selectedBills) {
            let selectedBill = this.selectedBills[billId]
            if(selectedBill.bill.company.fields.find(f => !f.billdataInput)) {
        return true
    }
        }
        return false
    }

    public isUtilitySelectedWithAdditionalField(company: Company) : boolean {
        return !!company.fields.find(f => !f.billdataInput)
    }

    public amountChange(ev) {
        let _this = $(ev.target)
        let amount = +_this.val()
        let id = _this.attr("bill-id")
        if(this.selectedBills.hasOwnProperty(id)) {
            if(amount == 0) {
                Snackbar.show("danger", "Amount can not be 0")
            }
            this.totalSelectedAmount -= this.selectedBills[id].amount
            this.totalSelectedAmount += amount
            this.selectedBills[id].amount = amount
        }
    }

    public toggleCurrentPage(isSelect: boolean) {
        this.toggleBills(this.bills.records, isSelect)
        if(this.selectedsShowing) {
            this.showSelecteds(this.bills.offset, this.bills.perPage)
        }
    }

    private toggleBills(bills: Bill[], isSelect: boolean) {
        let hasOutOfSyncBill = false
        bills.forEach(r => {
            if(isSelect) {
                if (!this.selectedBills.hasOwnProperty(r.id)) {
                    if (r.company.hasBill && this.lastSyncWasBeforeNDays(5, r)) {
                            hasOutOfSyncBill = true
                        }
                    this.selectBill(r, r.billAmount + r.serviceCharge)
                }
            } else {
                if (this.selectedBills.hasOwnProperty(r.id)) {
                    this.unSelectBill(r)
                }
            }
            return true
        })
        if(hasOutOfSyncBill) {
            Snackbar.show("warning", "There are some bills which were last synced more than 5 days ago")
        }
    }

    public clearSelection() {
        this.selectedBills = {}
        this.totalSelectedAmount = 0
        this.selectedCount = 0

        if(this.selectedsShowing) {
            let page = new PaginatorComponent();
            page.offset = 0
            page.currentPerPage = this.bills.perPage
            this.paymentService.loadTableData(page)
        }
    }

    public showSelecteds(offset: number, perPage: number) {
        if(this.selectedCount == 0) {
            Snackbar.show("warning", "No Selected Bills")
        }
        this.selectedsShowing = true
        this.bills = new PaginatedBill()
        this.bills.offset = offset || 0
        if(perPage) {
            this.bills.perPage = perPage
        }
        let selectedBills = Util.mapToArray(this.selectedBills, x => x.bill)
        this.bills.records = selectedBills.filter((v, i) => i >= this.bills.offset && i < this.bills.offset + this.bills.perPage)
        this.bills.count = selectedBills.length
    }

    public readAttachedFile(files: FileList, modal: ModalDirective) {
		let file : File = null;
		
	    if(files) {
			if(files.length > 1) { 
	            Snackbar.show("danger", "You have to choose only one file")
	            return;
	        }
	        file = files.item(0);
            if(file.size/1024/1024 > 5) { //5MB
                Snackbar.show("danger", "File size exceed than allowed size 5 MB")
                return
            }
            this.attachedFile = file
        }
    }

    public toggleFiltered(isSelect: boolean) {
        this.http.post<PaginatedBill>(app.basePath + '/paymentRequest/filteredBillDataList', $.extend({}, this.lastFilterData, {totalPerPage: -1})).subscribe(x => {
            x.records.forEach(y => {
                if(y.dueDate) {
                    y.dueDate = moment(y.dueDate, "DD/MM/YYYY HH:mm:ss").toDate()
                }
                if(y.syncDate) {
                    y.syncDate = moment(y.syncDate, "DD/MM/YYYY HH:mm:ss").toDate()
                }
            })
            this.toggleBills(x.records, isSelect)
        })
    }

    public changePageForSelecteds(page: PaginatorComponent) {
        this.showSelecteds(page.offset, page.currentPerPage)
    }

    public openAttachmentPopup(modal: ModalDirective) {
        this.attachedFile = null
    	modal.show()
  	}
  	
  	private sanitizedSelectedCellsSubmitData() : any {
  	    let submitableCells = $.extend(true, {}, this.selectedBills)
  	    for(let billId in submitableCells) {
  	        let cell = submitableCells[billId]
  	        delete cell.bill
  	    }
  	    return submitableCells
  	}

  	public isMaximumSelectedInCurrentPage() : boolean {
  	    return $(':checkbox:checked').length < $(':checkbox').length / 2
  	}
  	
  	public createPayment(modal: ModalDirective, attachedFile: File) {
        let hasZeroAmountRecords = false
        for(let id in this.selectedBills) {
            if(this.selectedBills[id].amount == 0) {
                hasZeroAmountRecords = true
                break;
            }
        }
        if(hasZeroAmountRecords) {
            Snackbar.show("danger", "There are some prepaid bills which have amount 0")
        } else {
            const formData: FormData = new FormData();
            formData.append('billsJson', JSON.stringify(this.sanitizedSelectedCellsSubmitData()));
            formData.append('attachment', attachedFile);
            this.tableDataLoading = true
            this.http.post<ApiPostResponse>(App.basePath + "/paymentRequest/initiateRequest", formData).subscribe(x => {
                Snackbar.show(x.code == 200 ? "success" : "danger", x.message);
                this.submit.emit(x)
                this.tableDataLoading = false
                if(App.user.permissions.includes(Actions.VIEW_REQUEST_STATUS)) {
                    this.router.navigateByUrl("/payment-request/request-status", {state: {start: moment().subtract(1, 'month').format("DD/MM/YYYY 00:00:00")}})
                } else {
                    this.router.navigateByUrl("/dashboard")
                }
            }, err => {
                this.tableDataLoading = false
            })
        }
    }

    public showHeaderAdditionalFieldsPopup(popup: any) {
        this.headerAdditionalFieldsDataModel = []
        for(let billId in this.selectedBills) {
            let selectedBill = this.selectedBills[billId]
            if(!this.headerAdditionalFieldsDataModel.find(h => h.id == selectedBill.bill.company.id)) {
                this.headerAdditionalFieldsDataModel.push(selectedBill.bill.company)
            }
        }
        popup.show()
    }

    public showRowAdditionalFieldsPopup(popup: any, bill: Bill) {
        this.rowAdditionalFieldsDataModel = {}
        let billSelected = false
        for(let billId in this.selectedBills) {
            let selectedBill = this.selectedBills[billId]
            if(selectedBill.bill.id == bill.id) {
                this.rowAdditionalFieldsDataModel.company = bill.company.name
                this.rowAdditionalFieldsDataModel.fields = []
                for(let field of bill.company.fields) {
                    if(!field.billdataInput) {
                        this.rowAdditionalFieldsDataModel.fields.push({id: field.id, bill_id: bill.id, name: field.paramName, value: selectedBill.additionalFields[field.paramCode]})
                        billSelected = true
                    }
                }
                break;
            }
        }
        if(!billSelected) {
            Snackbar.show("danger", "This bill is not selected for payment")
            return
        }
        popup.show()
    }

    public applyHeaderAdditionalFieldsValues(popup: any) {
        let body = $(this.headerAdditionalFieldModal.nativeElement)
        let amountValue = body.find(".header-amount-field").val()?.trim()
        if(amountValue && amountValue.match(/\d+/)) {
            for(let billId in this.selectedBills) {
                let selectedBill = this.selectedBills[billId]
                if(!selectedBill.bill.company.hasBill) {
                    this.totalSelectedAmount -= selectedBill.amount
                    selectedBill.amount = +amountValue
                    this.totalSelectedAmount += +amountValue
                }
            }
        }

        let additionalFieldTrs = body.find("tr[field-id]").toArray()
        for(let tr of additionalFieldTrs) {
            let value = $(tr).find("input").val().trim()
            if(value) {
                let companyId = +$(tr).attr("company-id")
                let fieldId = +$(tr).attr("field-id")
                for(let billId in this.selectedBills) {
                    let selectedBill = this.selectedBills[billId]
                    if(selectedBill.bill.company.id == companyId) {
                        for(let field of selectedBill.bill.company.fields) {
                            if(field.id == fieldId) {
                                selectedBill.additionalFields[field.paramCode] = value
                            }
                        }
                    }
                }
            }
        }

        body.find("input").val("")
        popup.hide()
    }

    public applyRowAdditionalFieldsValues(popup: any) {
        let body = $(this.rowAdditionalFieldModal.nativeElement)

        let additionalFieldTrs = body.find("tr[field-id]").toArray()
        for(let tr of additionalFieldTrs) {
            let value = $(tr).find("input").val().trim()
            let fieldId = +$(tr).attr("field-id")
            let billId = +$(tr).attr("bill-id")
            let selectedBill = this.selectedBills[billId]
            for(let field of selectedBill.bill.company.fields) {
                if(field.id == fieldId) {
                    selectedBill.additionalFields[field.paramCode] = value
                }
            }
        }

        popup.hide()
    }
}
