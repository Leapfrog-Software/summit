//
//  CardDetailViewController.swift
//  Summit
//
//  Created by Leapfrog-Software on 2018/04/30.
//  Copyright © 2018年 Leapfrog-Inc. All rights reserved.
//

import UIKit

class CardDetailViewController: UIViewController {

    enum CellType {
        case userInfo
        case title
        case schedule
        case noData
    }
    
    struct CellData {
        let cellType: CellType
        let title: String?
        let scheduleCount: Int?
        let scheduleData: ScheduleData?
    }
    
    @IBOutlet private weak var tableView: UITableView!
    @IBOutlet private weak var sendCardBaseView: UIView!
    @IBOutlet private weak var sendCardBaseViewHeightConstraint: NSLayoutConstraint!
    @IBOutlet private weak var sendCardButton: UIButton!

    private var userData: UserData!
    private var cellDatas = [CellData]()
    private var isShowSendCard = false
    
    func set(userData: UserData, showSendCard: Bool) {
        self.userData = userData
        self.isShowSendCard = showSendCard
        
        let today = Date()
        let reservedSchedules = userData.reserves.compactMap { ScheduleRequester.shared.query(id: $0) }
        
        self.cellDatas.append(CellData(cellType: .userInfo, title: nil, scheduleCount: nil, scheduleData: nil))
        
        let futureSchedules = reservedSchedules.filter { $0.date >= today }
        self.cellDatas.append(CellData(cellType: .title, title: "参加予定のイベント", scheduleCount: futureSchedules.count, scheduleData: nil))
        if futureSchedules.isEmpty {
            self.cellDatas.append(CellData(cellType: .noData, title: nil, scheduleCount: nil, scheduleData: nil))
        } else {
            futureSchedules.forEach {
                self.cellDatas.append(CellData(cellType: .schedule, title: nil, scheduleCount: nil, scheduleData: $0))
            }
        }
        
        let pastSchedules = reservedSchedules.filter { $0.date < today }
        self.cellDatas.append(CellData(cellType: .title, title: "過去に参加したイベント", scheduleCount: pastSchedules.count, scheduleData: nil))
        if pastSchedules.isEmpty {
            self.cellDatas.append(CellData(cellType: .noData, title: nil, scheduleCount: nil, scheduleData: nil))
        } else {
            pastSchedules.forEach {
                self.cellDatas.append(CellData(cellType: .schedule, title: nil, scheduleCount: nil, scheduleData: $0))
            }
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.tableView.rowHeight = UITableViewAutomaticDimension
        self.tableView.estimatedRowHeight = 200
        
        if self.isShowSendCard {
            if let myUserData = UserRequester.shared.myUserData() {
                if myUserData.cards.contains(self.userData.userId) {
                    self.sendCardButton.backgroundColor = UIColor.inActiveButton
                    self.sendCardButton.setTitle("名刺は送信済みです", for: .normal)
                    self.sendCardButton.isEnabled = false
                } else {
                    self.sendCardButton.backgroundColor = UIColor.activeButton
                }
            }
        } else {
            self.sendCardBaseView.isHidden = true
            self.sendCardBaseViewHeightConstraint.constant = 0
        }
    }
    
    @IBAction func onTapSendCard(_ sender: Any) {

        var cpUserData = self.userData!
        cpUserData.cards.append(SaveData.shared.userId)
        AccountRequester.updateUser(userData: cpUserData, completion: { result in
            if result {
                Dialog.show(style: .success, title: "確認", message: "名刺を送信しました", actions: [DialogAction(title: "OK", action: nil)])
                
                self.sendCardButton.backgroundColor = UIColor.inActiveButton
                self.sendCardButton.setTitle("名刺は送信済みです", for: .normal)
                self.sendCardButton.isEnabled = false
                
            } else {
                Dialog.show(style: .error, title: "エラー", message: "通信に失敗しました", actions: [DialogAction(title: "OK", action: nil)])
            }
        })
    }
    
    @IBAction func onTapBack(_ sender: Any) {
        self.pop(animationType: .horizontal)
    }
}

extension CardDetailViewController: UITableViewDataSource, UITableViewDelegate {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return self.cellDatas.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        let cellData = self.cellDatas[indexPath.row]
        
        switch cellData.cellType {
        case .userInfo:
            let cell = tableView.dequeueReusableCell(withIdentifier: "CardDetailUserInfoTableViewCell", for: indexPath) as! CardDetailUserInfoTableViewCell
            cell.configure(userData: self.userData)
            return cell
            
        case .title:
            let cell = tableView.dequeueReusableCell(withIdentifier: "CardDetailEventTitleTableViewCell", for: indexPath) as! CardDetailEventTitleTableViewCell
            cell.configure(title: cellData.title!, count: cellData.scheduleCount!)
            return cell
            
        case .schedule:
            let cell = tableView.dequeueReusableCell(withIdentifier: "CardDetailEventTableViewCell", for: indexPath) as! CardDetailEventTableViewCell
            cell.configure(scheduleData: cellData.scheduleData!)
            return cell
            
        case .noData:
            return tableView.dequeueReusableCell(withIdentifier: "CardDetailNoDataTableViewCell", for: indexPath)
        }
    }
}
