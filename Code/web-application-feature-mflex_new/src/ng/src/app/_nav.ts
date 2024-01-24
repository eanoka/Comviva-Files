import {INavData} from '@coreui/angular';
import {App} from './app.model'
import {Actions} from "./generic/actions.consts";

export let NavItemsPopulator = {
    populate: function () {
        const _navItems: INavData[] = [];
        if(!App.user) {
            return _navItems
        }

        const permissions: string[] = App.user.permissions

        const manageBillDataSection: INavData = {
            name: 'Manage Bill Data',
            url: '/manage-bill-data',
            children: []
        }
		const createBillDataMenu: INavData = {
			name: 'Create Bill Data',
            url: '/manage-bill-data/create-bill-data',
        }
		const editDataMenu: INavData = {
			name: (permissions.includes("Manage Bill Data") ? 'Edit' : 'View') + ' Bill Data',
            url: '/manage-bill-data/edit-data',
        }
		const validateBillDataMenu: INavData = {
			name: 'Validate Bill Data',
            url: '/manage-bill-data/validate-bill-data',
        }

		if (permissions.includes("Manage Bill Data")) {
            manageBillDataSection.children.push(createBillDataMenu)
        }
        if (permissions.includes("Manage Bill Data") || permissions.includes("View Bill Data")) {
            manageBillDataSection.children.push(editDataMenu)
        }
        if (permissions.includes("Validate Bill Data")) {        
        	manageBillDataSection.children.push(validateBillDataMenu)
        }
        
		if (manageBillDataSection.children.length) {
            _navItems.push(manageBillDataSection)
		}

        const paymentRequestSection: INavData = {
            name: 'Payment Request',
            url: '/payment-request',
            children: []
        }
		const checkBillStatusMenu: INavData = {
			name: 'Bill Status',
			url: '/payment-request/check-bill-status',
        }
        const refreshBillsMenu: INavData = {
            name: 'Refresh Bills',
            url: '/payment-request/refresh-bills',
        }
		const createPaymentRequestMenu: INavData = {
			name: 'Create Payment Request',
            url: '/payment-request/create',
        }
        const billCollectionRequest: INavData = {
			name: 'Bill Collection Request',
            url: '/payment-request/bill-collection-request',
        }
		const requestStatusMenu: INavData = {
			name: 'Request Status',
			url: '/payment-request/request-status',
        }
		
		if (permissions.includes(Actions.VIEW_BILL_STATUS)) {
			paymentRequestSection.children.push(checkBillStatusMenu)
        }
        if (permissions.includes("Create Payment Request")) {
            paymentRequestSection.children.push(refreshBillsMenu)
        }
        if (permissions.includes("Create Payment Request")) {
            paymentRequestSection.children.push(createPaymentRequestMenu)
        }
        paymentRequestSection.children.push(billCollectionRequest)
		if (permissions.includes(Actions.VIEW_REQUEST_STATUS)) {
			paymentRequestSection.children.push(requestStatusMenu)
        }
		if (paymentRequestSection.children.length) {
            _navItems.push(paymentRequestSection)
		}

        const paymentApprovalSection: INavData = {
            name: 'Payment Approval',
            url: '/payment-approval',
            children: [{
                name: 'Approval Status',
                url: '/payment-approval/approval-status',
            }, {
                name: 'Approve for Payment',
                url: '/payment-approval/approve-for-payment',
            }]
        }
		
		if (permissions.includes("$APPROVE_PAYMENT_ANY_LEVEL$")) {
            _navItems.push(paymentApprovalSection)
		}

        const billPaymentSection: INavData = {
            name: 'Bill Payment',
            url: '/payment',
            children: [{
                name: 'Approved Bill Pay Status',
                url: '/payment/approved-bill-pay-status',
            }, {
                name: 'Bill Payment',
                url: '/payment/bill-payment',
            }]
        }
		
		if (permissions.includes("Initiate Payment")) {
            _navItems.push(billPaymentSection)
		}

        const userManagementSection: INavData = {
            name: 'User Management',
            url: '/user-management',
            children: []
        }

		const createUserMenu: INavData = {
			name: 'Create User',
			url: '/user-management/create-user',
        }
		const barUnbarUserMenu: INavData = {
			name: 'Bar/UnBar User',
            url: '/user-management/bar-unbar-user',
        }
		const editUserMenu: INavData = {
			name: 'Edit User',
			url: '/user-management/edit-user',
        }
		const deleteUserMenu: INavData = {
			name: 'Delete User',
			url: '/user-management/delete-user',
        }
		const userReportMenu: INavData = {
			name: 'List Users',
			url: '/user-management/user-report',
        }
		const userPermMenu: INavData = {
			name: 'User Permissions',
			url: '/user-management/user-perms',
        }
		
		if (permissions.includes("Create User (Owner Account)")) {
            userManagementSection.children.push(createUserMenu)
        }
        if (permissions.includes("Bar User (Owner Account)")) {
            userManagementSection.children.push(barUnbarUserMenu)
        }
        if (permissions.includes("Edit User (Owner Account)")) {
            userManagementSection.children.push(editUserMenu)
        }
        if (permissions.includes("Delete User (Owner Account)")) {
            userManagementSection.children.push(deleteUserMenu)
        }
        if (permissions.includes("List Users (Owner Account)")) {
            userManagementSection.children.push(userReportMenu)
        }
        if (permissions.includes(Actions.EDIT_USER) || permissions.includes(Actions.EDIT_PERMISSION)) {
            userManagementSection.children.push(userPermMenu)
        }
        if (userManagementSection.children.length) {
            _navItems.push(userManagementSection)
        }

        const roleManagementSection: INavData = {
            name: 'Role Management',
            url: '/role-management',
            children: []
        }

        const createRoleMenu: INavData = {
            name: 'Create Role',
            url: '/role-management/create-role',
        }
        const editRoleMenu: INavData = {
            name: 'Edit Role',
            url: '/role-management/edit-role',
        }
        const deleteRoleMenu: INavData = {
            name: 'Delete Role',
            url: '/role-management/delete-role',
        }
        const roleListMenu: INavData = {
            name: 'Role List',
            url: '/role-management/role-list',
        }
        const rolePermMenu: INavData = {
            name: 'Role Permissions',
            url: '/role-management/role-perms',
        }

        if (permissions.includes("Manage Role")) {
            roleManagementSection.children.push(createRoleMenu)
            roleManagementSection.children.push(editRoleMenu)
            roleManagementSection.children.push(deleteRoleMenu)
            roleManagementSection.children.push(roleListMenu)
            if (permissions.includes(Actions.EDIT_PERMISSION)) {
                roleManagementSection.children.push(rolePermMenu)
            }
        }
        if (userManagementSection.children.length) {
            _navItems.push(roleManagementSection)
        }
     
	   const accountManagementSection: INavData = {
            name: 'Account Management',
            url: '/account-management',
            children: []
        }
        const createAccountMenu: INavData = {
            name: 'Create ' + (App.user.isGP ? '' : 'Sub ') + 'Account',
            url: '/account-management/create-account',
        }
        const barAccountMenu: INavData = {
            name: 'Bar/Unbar Account',
            url: '/account-management/bar-unbar-account',
        }
        const checkBalanceMenu: INavData = {
            name: 'Check Balance',
            url: '/account-management/check-balance',
        }
        const billPaymentApprovalMenu: INavData = {
            name: 'Bill Payment Approvals',
            url: '/account-management/billpayment-approval',
        }
        const billDataValidatorMenu: INavData = {
            name: 'Bill Data Validator',
            url: '/account-management/billdata-validator',
        }

        if (permissions.includes("Manage Account")) {
            accountManagementSection.children.push(createAccountMenu)
            if(App.user.isGP) {
                accountManagementSection.children.push(barAccountMenu)
            }
        }
        if (permissions.includes("Check Balance")) {
            accountManagementSection.children.push(checkBalanceMenu)
        }
        if(App.user.role.name.includes("Client Admin")) {
        	accountManagementSection.children.push(billPaymentApprovalMenu)
        	accountManagementSection.children.push(billDataValidatorMenu)
        }
        if (accountManagementSection.children.length) {
            _navItems.push(accountManagementSection)
        }

	   const transactionReportsData: INavData = {
            name: 'Transaction Reports',
            url: '/transaction-reports',
            children: []
        }
		
		const detailTransactionReportMenu: INavData = {
			name: 'Detail Transaction Report',
			url: '/transaction-reports/detail-transaction-report',
		}
		
		const prepaidTokenEnquiryMenu: INavData = {
            name: 'Prepaid Token Enquiry',
			url: '/transaction-reports/prepaid-token-enquiry',
        }
        
        const vatReportMenu: INavData = {
            name: 'Vat Report',
			url: '/transaction-reports/vat-report',
        }
        transactionReportsData.children.push(vatReportMenu)
        
        if (permissions.includes("View Transaction Report")) {
            transactionReportsData.children.push(detailTransactionReportMenu)
        }
        if (permissions.includes("View Prepaid Token Status")) {
            transactionReportsData.children.push(prepaidTokenEnquiryMenu)
        }
		if (transactionReportsData.children.length) {
            _navItems.push(transactionReportsData)
		}
        return _navItems
    }
}