package jpabook.jpashop.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.QMember;
import jpabook.jpashop.domain.QOrder;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

	private final EntityManager em;
	
	public void save(Order order) {
		em.persist(order);
	}
	
	public Order findOne(Long id) {
		return em.find(Order.class, id);
	}
	
//	public List<Order> findAll(OrderSearch orderSearch) {
//		
//		return em.createQuery("select o from Order o join o.member m"+
//				" where o.status = :status" +
//				" and m.name like :name", Order.class)
//				.setParameter("status", orderSearch.getOrderStatus())
//				.setParameter("name", orderSearch.getMemberName())
//				.setMaxResults(1000)
//				.getResultList();
//		
//	}

	public List<Order> findAll(OrderSearch orderSearch) {
		JPAQueryFactory query = new JPAQueryFactory(em);
		QOrder order = QOrder.order;
		QMember member = QMember.member;
		
		return query
				.select(order)
				.from(order)
				.join(order.member, member)
				.where(statusEq(orderSearch.getOrderStatus()),
						 nameLike(orderSearch.getMemberName()))
				.limit(1000)
				.fetch();
	}
	
	private BooleanExpression statusEq(OrderStatus statusCond) {
		if (statusCond == null) {
			return null;
		}
		return QOrder.order.status.eq(statusCond);
	}

	private BooleanExpression nameLike(String nameCond) {
		if (!StringUtils.hasText(nameCond)) {
			return null;
		}
		return QMember.member.name.like(nameCond);
	}
	
	public List<Order> findAllByString(OrderSearch orderSearch) {
		
		// language=JPAQL
		String jpql = "select o From Order o join o.member m";
		boolean isFirstCondition = true;
		
		// 주문 상태 검색
		if (orderSearch.getOrderStatus() != null) {
			if (isFirstCondition) {
				jpql += " where";
				isFirstCondition = false;
			} else {
				jpql += " and";
			}
			jpql += " o.status = :status";
		}
		
		// 회원 이름 검색
		if (StringUtils.hasText(orderSearch.getMemberName())) {
			if (isFirstCondition) {
				jpql += " where";
				isFirstCondition = false;
			} else {
				jpql += " and";
			}
			jpql += " m.name like :name";
		}
		
		TypedQuery<Order> query = em.createQuery(jpql, Order.class).setMaxResults(1000); // 최대 1000건
		
		if (orderSearch.getOrderStatus() != null) {
			query = query.setParameter("status", orderSearch.getOrderStatus());
		}
		
		if (StringUtils.hasText(orderSearch.getMemberName())) {
			query = query.setParameter("name", orderSearch.getMemberName());
		}
		
		return query.getResultList();
		
	}
			
	/*
	 * JPA Criteria
	 */
	public List<Order> findAllByCriteria(OrderSearch orderSearch) {
		
		 CriteriaBuilder cb = em.getCriteriaBuilder();
		 CriteriaQuery<Order> cq = cb.createQuery(Order.class);
		 Root<Order> o = cq.from(Order.class);
		 Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
		 
		 List<Predicate> criteria = new ArrayList<>();
		 
		 //주문 상태 검색
		 if (orderSearch.getOrderStatus() != null) {
			 
			Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
			criteria.add(status);
			
		 }
		 
		 //회원 이름 검색
		 if (StringUtils.hasText(orderSearch.getMemberName())) {
			 
			Predicate name = cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName() + "%");
			criteria.add(name);
			
		 }
		 
		 cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
		 TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //최대 1000건
		 
		 return query.getResultList();
		 
	}
	
	public List<Order> findAllWithMemberDelivery() {
		return em.createQuery(
				 "select o from Order o" +
				 " join fetch o.member m" +
				 " join fetch o.delivery d", Order.class)
				 .getResultList();
	}

	//DB의 dinstinct 모든 컬럼이 같아야 중복 제거 하고 JPA에 distinct는 자체적으로 같은 아이디 값이면 중복을 제거한다.
	public List<Order> findAllWithItem() {
		return em.createQuery(
				 "select distinct o from Order o" +
						 " join fetch o.member m" +
						 " join fetch o.delivery d" +
						 " join fetch o.orderItems oi" +
						 " join fetch oi.item i", Order.class)
						 .getResultList();
	}
	
	public List<Order> findAllWithMemberDelivery(int offset, int limit) {
		return em.createQuery("select o from Order o" + " join fetch o.member m" + " join fetch o.delivery d",
				Order.class)
				.setFirstResult(offset)
				.setMaxResults(limit)
				.getResultList();
	}
}
